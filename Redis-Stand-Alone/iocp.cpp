#include "iocp.h"

/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::PostAcceptEx

  Summary:  accept the connection, and allocate I/O info structure

  Args:     PPER_IO_INFO p_acce_io_info
              I/O info structure that descripes the connection

  Modifies: [p_acce_io_info, link_pool]

  Returns:  OPSTATUS
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
OPSTATUS IOCP::PostAcceptEx( PPER_IO_INFO p_acce_io_info )
{
    ULONG u_bytes_ret = 0;
    if ( (*(PPER_LINK_INFO *)p_acce_io_info->buffer = link_pool.LinkPoolAlloc()) == NULL )
    {
        return OP_FAILED;
    }
    ZeroMemory( &p_acce_io_info->overlapped, sizeof(OVERLAPPED) );

    BOOL bRet = p_AcceptEx( p_ser_link_info->socket, ( *(PPER_LINK_INFO *)p_acce_io_info->buffer )->socket, &p_acce_io_info->buffer[sizeof(PPER_LINK_INFO)], 0, sizeof(SOCKADDR_IN) + 16, sizeof(SOCKADDR_IN) + 16, &u_bytes_ret, &p_acce_io_info->overlapped );
    if ( !bRet && WSAGetLastError() != WSA_IO_PENDING )
    {
        printf( "#Err: post AcceptEx failed\n" );
        return OP_FAILED;
    }

    return OP_SUCCESS;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::AgingThread

  Summary:  manage connections status, close overtimed connections

  Args:     LPVOID arg_list
              contain the "this" pointer of IOCP instance

  Modifies: [link_pool]

  Returns:  UINT
              thread termination status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
UINT WINAPI IOCP::AgingThread( LPVOID arg_list )
{
    IOCP* p_this = static_cast<IOCP *>(arg_list);

    while( TRUE )
    {
        Sleep( 1000 );
        p_this->link_pool.LinkPoolCheck(p_this->p_DisconnectEx);
    }
    return 0;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::PostRecv

  Summary:  post receive on the socket
            return only after receive the size of package header

  Args:     PPER_IO_INFO p_per_link_info
              I/O info structure that descripes the connection

            ULONG buff_offset
              indicate where the newly received data should be written to

            ULONG buff_len
              indicate the length of data that suppoused to receive,
              function can return without receiving enough data

  Modifies: [p_per_link_info]

  Returns:  OPSTATUS
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
OPSTATUS IOCP::PostRecv( PPER_LINK_INFO p_per_link_info, ULONG buff_offset, ULONG buff_len )
{
    ULONG u_flag = 0;
    ULONG u_recv = 0;

    ZeroMemory( &p_per_link_info->p_per_io_info[0].overlapped, sizeof(OVERLAPPED) );
    p_per_link_info->p_per_io_info[0].w_buf.buf = &p_per_link_info->p_per_io_info[0].buffer[buff_offset];
    p_per_link_info->p_per_io_info[0].w_buf.len = buff_len;

    if( WSARecv( p_per_link_info->socket, &p_per_link_info->p_per_io_info[0].w_buf, 1, &u_recv, &u_flag, &p_per_link_info->p_per_io_info[0].overlapped, NULL ) == SOCKET_ERROR &&
        WSAGetLastError() != WSA_IO_PENDING )
    {
        printf( "#Err: post receive failed, discard connection\n" );
        return OP_FAILED;
    }

    return OP_SUCCESS;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::IsRecvFinish

  Summary:  check whether data is received completely
            if not, post receive on the socket

  Args:     PPER_IO_INFO p_per_link_info
              I/O info structure that descripes the connection

            ULONG actual_trans
              indicate the amount of data has been received so far

  Modifies: [p_per_link_info]

  Returns:  OPSTATUS
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
OPSTATUS IOCP::IsRecvFinish( PPER_LINK_INFO p_per_link_info, ULONG actual_trans )
{
    // package length cannot bigger than buffer 
    if ( p_per_link_info->p_per_io_info[0].w_buf.len != actual_trans )
    {
        // not completely, post receive
        p_per_link_info->p_per_io_info[0].curr_data_len += actual_trans;
        PostRecv( p_per_link_info, actual_trans, p_per_link_info->p_per_io_info[0].w_buf.len - actual_trans );
        p_per_link_info->p_per_io_info[0].post_recv_times ++;
        return OP_FAILED;
    }
    else
    {
        // received completely, change reveived length
        p_per_link_info->p_per_io_info[0].curr_data_len += actual_trans;
    }

    // package length bigger than buffer, discard the link directly
    if ( ((PPACKET_HEADER)( p_per_link_info->p_per_io_info[0].buffer ))->packet_len >= sizeof(PACKET_HEADER) && ((PPACKET_HEADER)( p_per_link_info->p_per_io_info[0].buffer ))->packet_len > p_per_link_info->p_per_io_info[0].curr_data_len)
    {
        // not completely, post receive
        PostRecv( p_per_link_info, p_per_link_info->p_per_io_info[0].curr_data_len, ((PPACKET_HEADER)( p_per_link_info->p_per_io_info[0].buffer ))->packet_len - p_per_link_info->p_per_io_info[0].curr_data_len );
        p_per_link_info->p_per_io_info[0].post_recv_times ++;
        return OP_FAILED;
    }

    return OP_SUCCESS;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::AcceptClient

  Summary:  accept new connection
            post receive on the new connection

  Args:     PPER_IO_INFO p_per_io_Info
              I/O info structure that descripes the connection

  Modifies: [p_per_io_Info]

  Returns:  OPSTATUS
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
OPSTATUS IOCP::AcceptClient(PPER_IO_INFO p_per_io_Info)
{
    PPER_LINK_INFO p_per_link_info = *(PPER_LINK_INFO *)p_per_io_Info->buffer;

    if ( CreateIoCompletionPort( (HANDLE)p_per_link_info->socket, h_iocp, (ULONG_PTR)p_per_link_info, 0 ) == NULL )
    {
        printf( "#Err: accept client failed\n" );
        p_DisconnectEx( p_per_link_info->socket, NULL, TF_REUSE_SOCKET, 0 );
        return OP_FAILED;
    }

    p_per_link_info->state_machine = SM_FULL;
    PostAcceptEx(p_per_io_Info);
    PostRecv( p_per_link_info, 0, sizeof(PACKET_HEADER) );

    return OP_SUCCESS;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::PacketSend

  Summary:  asynchronously send data that stored in the send buffer

  Args:     PPER_IO_INFO p_per_io_Info
              I/O info structure that descripes the connection

  Modifies: [p_per_link_info]

  Returns:  BOOL
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
BOOL IOCP::PacketSend(PPER_LINK_INFO p_per_link_info)
{
    ULONG u_send = 0;
    ULONG u_flag = 0;

    ZeroMemory(&p_per_link_info->p_per_io_info[1].overlapped, sizeof(OVERLAPPED));

    EnterCriticalSection(&SendCriticalSection);
    ULONG uRet = WSASend(p_per_link_info->socket, &p_per_link_info->p_per_io_info[1].w_buf, 1, &u_send, u_flag, &p_per_link_info->p_per_io_info[1].overlapped, NULL);
    if (uRet == SOCKET_ERROR && WSAGetLastError() != WSA_IO_PENDING)
    {
        printf("#Err: send data package asynchronously failed\n");
        LeaveCriticalSection(&SendCriticalSection);

        return FALSE;
    }
    LeaveCriticalSection(&SendCriticalSection);

    return TRUE;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::LogonStatus

  Summary:  nodify client the logon status

  Args:     PPER_IO_INFO p_per_link_info
              I/O info structure that descripes the connection

            ULONG status
              logon status

  Modifies: [p_per_link_info]

  Returns:  BOOL
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
BOOL IOCP::LogonStatus(PPER_LINK_INFO p_per_link_info, ULONG status)
{
    ZeroMemory(p_per_link_info->p_per_io_info[1].buffer, MAX_BUF_LEN);

    ((PPACKET_HEADER)p_per_link_info->p_per_io_info[1].buffer)->comm_code = status;
    ((PPACKET_HEADER)p_per_link_info->p_per_io_info[1].buffer)->packet_len = sizeof(PACKET_HEADER);

    p_per_link_info->p_per_io_info[1].w_buf.len = sizeof(PACKET_HEADER);

    return PacketSend(p_per_link_info);
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::DealThread

  Summary:  thread used to accept connection and process messages

  Args:     LPVOID arg_list
              contain the "this" pointer of IOCP instance

  Modifies: [link_pool, p_redis]

  Returns:  UINT
              thread termination status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
UINT WINAPI IOCP::DealThread( LPVOID arg_list )
{
    IOCP* p_this = static_cast<IOCP *>(arg_list);

    ULONG actual_trans = 0;
    OVERLAPPED *p_overlapped = NULL;   
    PPER_IO_INFO p_per_io_Info = NULL;
    PPER_LINK_INFO p_per_link_info = NULL;

    // exclusive redis instance
    RedisConnector* p_redis = new RedisConnector(p_this->redis_addr.c_str(), p_this->redis_port);
    p_redis->Connect();

    while( TRUE )
    {
        GetQueuedCompletionStatus( p_this->h_iocp, &actual_trans, (PULONG_PTR)&p_per_link_info, &p_overlapped, INFINITE );
        p_per_io_Info = (PPER_IO_INFO)CONTAINING_RECORD(p_overlapped, PER_IO_INFO, overlapped );

        if(p_per_io_Info->op_type == IO_ACCE )
        {
            printf("Received Client Connection Request\n");
            p_this->AcceptClient(p_per_io_Info);
        }
        else if(p_per_io_Info->op_type == IO_RECV )
        {
            // break, if the package received is not complete
            if ( p_this->IsRecvFinish( p_per_link_info, actual_trans ) == OP_FAILED ) continue;

            // receive completed. prepare for next package receiving
            p_per_link_info->p_per_io_info[0].w_buf.len = sizeof(PACKET_HEADER);
            p_per_link_info->p_per_io_info[0].w_buf.buf = p_per_link_info->p_per_io_info[0].buffer;
            p_per_link_info->p_per_io_info[0].curr_data_len = 0;

            // do different thing for different communication code
            switch( ((PPACKET_HEADER)p_per_io_Info->buffer)->comm_code )
            {
                case MSG_LOGON:
                {
                    if (p_this->CheckDeviceID(((PPACKET_LOGON)p_per_io_Info->buffer)->account))
                    {
                        printf("#Log: logon succeed\nDevice: %s\n", ((PPACKET_LOGON)p_per_io_Info->buffer)->account);
                        strcpy_s(p_per_link_info->client_info.account, ((PPACKET_LOGON)p_per_io_Info->buffer)->account);
                        p_this->LogonStatus(p_per_link_info, MSG_LOGON_SUCCESS);
                    }
                    else
                    {
                        printf("#Log: logon failed\nDevice: %s\n", ((PPACKET_LOGON)p_per_io_Info->buffer)->account);
                        p_this->LogonStatus(p_per_link_info, MSG_LOGON_FAILURE);
                    }

                    break;
                }
                case MSG_LOGOUT:
                {
                    printf("#Log: Device:  %s  logged out\n", p_per_link_info->client_info.account);

                    p_this->p_DisconnectEx(p_per_link_info->socket, NULL, TF_REUSE_SOCKET, 0);
                    p_per_link_info->free_flag = LINK_FREE;
                    p_per_link_info->state_machine = SM_IDLE;
                    p_per_link_info->heartbeat_info.hold_time = 0;
                    continue;
                }
                case MSG_GEO_LOCATION:
                {
                    Value& detection = p_this->config["detection"];
                    string range = detection["radius"].GetString();
                    stringstream location;
                    location << detection["longitude"].GetString() << " " << detection["latitude"].GetString();

                    BOOL prev_status = p_redis->DetectGeofence(location.str(), range, string(p_per_link_info->client_info.account));
                    PPACKET_GEO_LOCATION p_location = (PPACKET_GEO_LOCATION)p_per_io_Info->buffer;
                    p_redis->InsertGeospatial(p_location, string(p_per_link_info->client_info.account));
                    BOOL curr_status = p_redis->DetectGeofence(location.str(), range, string(p_per_link_info->client_info.account));

                    if (prev_status && !curr_status)
                    {
                        printf("Device %s is leaving the geofence\n", p_per_link_info->client_info.account);
                    }
                    else if (!prev_status && curr_status)
                    {
                        printf("Device %s is entering the geofence\n", p_per_link_info->client_info.account);
                    }

                    break;
                }
                case MSG_EVENT:
                {
                    PPACKET_EVENT p_event = (PPACKET_EVENT)p_per_io_Info->buffer;
                    p_redis->InsertEvent(p_event, string(p_per_link_info->client_info.account));
                    break;
                }
                case MSG_HEART_BEAT:
                {
                    break;
                }
                default:
                {
                    break;
                }
            }

            if (p_per_link_info->state_machine == SM_FULL || p_per_link_info->state_machine == SM_OVER)
            {
                p_per_link_info->state_machine = SM_FULL;
                p_per_link_info->heartbeat_info.hold_time = 240;
            }
            p_this->PostRecv(p_per_link_info, 0, sizeof(PACKET_HEADER));
        }
    }
    return 0;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::GenerateGeospatialReportThread

  Summary:  thread used to constantly check redis and generate report

  Args:     LPVOID arg_list
              contain the "this" pointer of IOCP instance

  Modifies: [link_pool, p_redis]

  Returns:  UINT
              thread termination status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
UINT WINAPI IOCP::GenerateGeospatialReportThread( LPVOID arg_list )
{
    IOCP* p_this = static_cast<IOCP*>(arg_list);
    RedisConnector* p_redis = new RedisConnector(p_this->redis_addr.c_str(), p_this->redis_port);
    p_redis->Connect();

    while (TRUE)
    {
        Value& detection = p_this->config["detection"];
        Value& device_ids = p_this->config["device_ids"];
        string range = detection["radius"].GetString();
        stringstream location;
        location << detection["longitude"].GetString() << " " << detection["latitude"].GetString();

        // Detect n objects within x feet of each other
        // here we iterate all devices and find all devices that within x feet, which include itself.
        string result_a;
        p_redis->DetectObjectInRange(device_ids, range, result_a);

        // Count objects passing a radius from a given position
        string result_b;
        p_redis->CountObjectInRange(location.str(), range, result_b);

        // save report locally
        ofstream ofs("geospatial_report.txt", ios::out | ios::trunc);
        ofs << result_a << endl << endl << result_b;
        ofs.close();

        Sleep(1000);
    }
    return 0;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::GenerateEventReportThread

  Summary:  thread used to constantly check redis and generate report

  Args:     LPVOID arg_list
              contain the "this" pointer of IOCP instance

  Modifies: [link_pool, p_redis]

  Returns:  UINT
              thread termination status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
UINT WINAPI IOCP::GenerateEventReportThread( LPVOID arg_list )
{
    IOCP* p_this = static_cast<IOCP*>(arg_list);
    RedisConnector* p_redis = new RedisConnector(p_this->redis_addr.c_str(), p_this->redis_port);
    p_redis->Connect();

    while (TRUE)
    {
        Value& tem = p_this->config["temporal"];
        Value& seq = p_this->config["sequence"];
        Value& eva = p_this->config["evaluation"];
        Value& devs = p_this->config["device_ids"];

        string result_a;
        p_redis->Temporal(tem, devs, result_a);

        string result_b;
        p_redis->Sequence(seq, devs, result_b);

        string result_c;
        p_redis->Evaluation(eva, devs, result_c);

        ofstream ofs("event_report.txt", ios::out | ios::trunc);
        ofs << "---------------Temporal---------------" << endl;
        ofs << result_a << endl << endl;
        ofs << "---------------Sequence---------------" << endl;
        ofs << result_b << endl << endl;
        ofs << "---------------Evaluation---------------" << endl;
        ofs << result_c;
        ofs.close();
        Sleep(1000);
    }
    return 0;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::InitialEnvironment

  Summary:  initial linkpool for I/O info structure allocation
            initial server I/O info structure
            dynamically load socket related functions from library

  Modifies: [link_pool, p_ser_link_info, p_acce_io_info, SendCriticalSection
             p_AcceptEx, p_DisconnectEx, p_GetAcceptExSockAddrs]

  Returns:  OPSTATUS
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
OPSTATUS IOCP::InitialEnvironment()
{
    // initial link pool
    link_pool.LinkPoolBuild();

    // allocate link info for server
    p_ser_link_info = (PPER_LINK_INFO)VirtualAlloc(NULL, sizeof(PER_LINK_INFO), MEM_COMMIT, PAGE_READWRITE);
    p_acce_io_info = (PPER_IO_INFO)VirtualAlloc(NULL, 10 * sizeof(PER_IO_INFO), MEM_COMMIT, PAGE_READWRITE);

    if ( p_ser_link_info == NULL || p_acce_io_info == NULL )
    {
        printf( "#Err: allocating server io info failed\n" );
        return OP_FAILED;
    }

    ZeroMemory( p_ser_link_info, sizeof(PER_LINK_INFO) );
    ZeroMemory( p_acce_io_info, 10 * sizeof(PER_IO_INFO) );

    p_ser_link_info->p_per_io_info = p_acce_io_info;

    // initial socket for server
    p_ser_link_info->socket = WSASocket( AF_INET, SOCK_STREAM, IPPROTO_TCP, NULL, 0, WSA_FLAG_OVERLAPPED );
    if ( p_ser_link_info->socket == NULL )
    {
        printf( "#Err: creating server socket failed\n" );
        return OP_FAILED;
    }

    // load lib functions
    ULONG uBytesRet = 0;
    GUID GuidAcceptEx = WSAID_ACCEPTEX;
    GUID GuidGetAcceptExSockAddrs = WSAID_GETACCEPTEXSOCKADDRS;
    GUID GuidDisconnectEx = WSAID_DISCONNECTEX;

    WSAIoctl(p_ser_link_info->socket, SIO_GET_EXTENSION_FUNCTION_POINTER, &GuidAcceptEx, sizeof(GuidAcceptEx), &p_AcceptEx, sizeof(p_AcceptEx), &uBytesRet, NULL, NULL);
    WSAIoctl(p_ser_link_info->socket, SIO_GET_EXTENSION_FUNCTION_POINTER, &GuidDisconnectEx, sizeof(GuidDisconnectEx), &p_DisconnectEx, sizeof(p_DisconnectEx), &uBytesRet, NULL, NULL);
    WSAIoctl(p_ser_link_info->socket, SIO_GET_EXTENSION_FUNCTION_POINTER, &GuidGetAcceptExSockAddrs, sizeof(GuidGetAcceptExSockAddrs), &p_GetAcceptExSockAddrs, sizeof(p_GetAcceptExSockAddrs), &uBytesRet, NULL, NULL);

    if (p_AcceptEx == NULL || p_DisconnectEx == NULL || p_GetAcceptExSockAddrs == NULL)
    {
        printf("#Err: unable to load lib functions <AcceptEx, DisconnectEx, GetAcceptExSockAddrs>\n");
        return OP_FAILED;
    }

    InitializeCriticalSection(&SendCriticalSection);

    // load config file
    ifstream ifs("config.json");
    if (ifs.is_open())
    {
        stringstream sstr;
        sstr << ifs.rdbuf();
        ifs.close();

        config.Parse(sstr.str().c_str());
    }
    else
    {
        printf("#Err: unable to load config file <config.json>\n");
        return OP_FAILED;
    }

    this->server_addr = this->config["server_address"].GetString();
    this->server_port = this->config["server_port"].GetInt();
    this->redis_addr = this->config["redis_address"].GetString();
    this->redis_port = this->config["redis_port"].GetInt();

    return OP_SUCCESS;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::CompletePortStart

  Summary:  start listening on the given address and port
            start 10 IOCP threads for network events processing

  Modifies: [link_pool, p_redis, h_iocp]

  Returns:  OPSTATUS
              operation status
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
OPSTATUS IOCP::CompletePortStart()
{
    SOCKADDR_IN sock_addr = {0};
    sock_addr.sin_family = AF_INET;
    sock_addr.sin_port = htons(this->server_port);
    sock_addr.sin_addr.S_un.S_addr = inet_addr(this->server_addr.c_str() );

    // bind socket to address & port
    if ( bind( p_ser_link_info->socket, (PSOCKADDR)&sock_addr, sizeof(SOCKADDR_IN) ) != 0 )
    {
        printf( "#Err: binding server socket failed\n" );
        return OP_FAILED;
    }

    // create compltion port
    h_iocp = CreateIoCompletionPort( INVALID_HANDLE_VALUE, NULL, 0, 0 );
    if ( h_iocp == NULL )
    {
        printf( "#Err: create IOCP failed\n" );
        return OP_FAILED;
    }

    // bind socket to compltion port
    if ( CreateIoCompletionPort( (HANDLE)p_ser_link_info->socket, h_iocp, (ULONG)p_ser_link_info, 0 ) == NULL )
    {
        printf( "#Err: binding IOCP failed\n" );
        return OP_FAILED;
    }

    // start listen
    if ( listen( p_ser_link_info->socket, SOMAXCONN ) == SOCKET_ERROR )
    {
        printf( "#Err: start listening socket failed\n" );
        return OP_FAILED;
    }

    // create 10 deal threads
    for (ULONG i = 0; i < 10; i ++)
    {
        printf("#Log: start IOCP deal thread #%u\n", i + 1);
        if ( ( h_thread[i] = (HANDLE)_beginthreadex( NULL, 0, IOCP::DealThread, this, 0, NULL ) ) == NULL )
        {
            printf( "#Err: start IOCP thread failed\n" );
            return OP_FAILED;
        }
    }

    // create aging thread
    if ( ( h_thread[10] = (HANDLE)_beginthreadex( NULL, 0, IOCP::AgingThread, this, 0, NULL ) ) == NULL )
    {
        printf( "#Err: start IOCP aging thread failed\n" );
        return OP_FAILED;
    }
    printf("#Log: start IOCP aging thread\n");

    for ( ULONG i = 0; i < 10; ++ i )
    {
        p_acce_io_info[i].op_type = IO_ACCE;
        p_acce_io_info[i].w_buf.len = MAX_BUF_LEN;
        p_acce_io_info[i].w_buf.buf = p_acce_io_info[i].buffer;

        if( PostAcceptEx( &p_acce_io_info[i] ) != OP_SUCCESS )
        {
            return OP_FAILED;
        }
    }

    // create geospatial report thread
    if ((h_thread[11] = (HANDLE)_beginthreadex(NULL, 0, IOCP::GenerateGeospatialReportThread, this, 0, NULL)) == NULL)
    {
        printf("#Err: start Geospatial Report thread failed\n");
        return OP_FAILED;
    }
    printf("#Log: start Geospatial Report thread\n");

    // create event report thread
    if ((h_thread[12] = (HANDLE)_beginthreadex(NULL, 0, IOCP::GenerateEventReportThread, this, 0, NULL)) == NULL)
    {
        printf("#Err: start Event Report thread failed\n");
        return OP_FAILED;
    }
    printf("#Log: start Event Report thread\n");

    while (TRUE)
    {
        Sleep(1000);
    }

    return OP_SUCCESS;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::InitialRedis

  Summary:  based on the device_ids, create corresponding tables

  Modifies: [p_redis]

  Returns:  BOOL
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
BOOL IOCP::InitialRedis()
{
    // TS.CREATE event:A:AB1345ED79 RETENTION 60000 DUPLICATE_POLICY MAX LABELS event_type A device_id AB1345ED79
    Value& device_ids = config["device_ids"];
    Value& event_types = config["event_types"];

    for (SizeType i = 0; i < device_ids.Size(); i++)
    {
        for (SizeType j = 0; j < event_types.Size(); j++)
        {
            stringstream commands;
            commands << "TS.CREATE event:" << event_types[j].GetString() << ":" << device_ids[i].GetString() << " RETENTION 0 LABELS event_type " << event_types[j].GetString() << " device_id " << device_ids[i].GetString();
            p_redis->ExecuteCommand(commands.str());
        }
    }

    return TRUE;
}


/*M+M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M+++M
  Method:   IOCP::CheckDeviceID

  Summary:  check whether the incoming device is registered

  Args:     const char* device_id

  Modifies: []

  Returns:  BOOL
M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M---M-M*/
BOOL IOCP::CheckDeviceID(const char* device_id)
{
    Value& device_ids = config["device_ids"];
    for (SizeType i = 0; i < device_ids.Size(); i++)
    {
        if (strcmp(device_ids[i].GetString(), device_id) == 0)
        {
            return TRUE;
        }
    }

    return FALSE;
}


IOCP::IOCP()
{
    InitialEnvironment();
    printf("------------------- Test Redis -------------------\n");
    p_redis = new RedisConnector(this->redis_addr.c_str(), this->redis_port);
    p_redis->Connect();
    p_redis->TestRedis();
}


IOCP::~IOCP()
{
    link_pool.LinkPoolDestroy();
    VirtualFree(p_acce_io_info, 0, MEM_RELEASE);
    VirtualFree(p_ser_link_info, 0, MEM_RELEASE);
}


int main(int argc, char const *argv[])
{
    IOCP server;
    printf("\n\n------------------- Start  IOCP -------------------\n");
    server.InitialRedis();
    server.CompletePortStart();
    return 0;
}
