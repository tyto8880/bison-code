#ifndef __PRE_DEFINE_H
#define __PRE_DEFINE_H

#include <stdio.h>
#include <process.h>
#include <winsock2.h>
#include <MSWSock.h>
#include <Windows.h>
#include <Iphlpapi.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include "../rapidjson/document.h"
#include "../rapidjson/writer.h"
#include "../rapidjson/stringbuffer.h"
using namespace rapidjson;
using namespace std;

#pragma comment(lib, "ws2_32.lib")
#pragma comment(lib, "Iphlpapi.lib")

typedef INT OPSTATUS;
#define OP_SUCCESS 0x0
#define OP_FAILED  0x1

#define MAX_BUF_SEND        8192
#define MAX_BUF_RECV        8192
#define MAX_BUF_LEN         (MAX_BUF_SEND + MAX_BUF_RECV)

#define IO_SEND             0x1
#define IO_RECV             0x2
#define IO_ACCE             0x3

#define LINK_FREE           0x0
#define LINK_BUSY           0x1

#define SM_IDLE             0x0
#define SM_FULL             0x1
#define SM_OVER             0x2
#define SM_FAIL             0x3

#define MSG_HEART_BEAT		0x1
#define MSG_LOGON			0x2
#define MSG_LOGOUT			0x3
#define MSG_LOGON_SUCCESS	0x4
#define MSG_LOGON_FAILURE	0x5
#define MSG_GEO_LOCATION	0x6
#define MSG_EVENT			0x7

typedef struct _PER_IO_INFO
{
	ULONG       op_type;
	ULONG       curr_data_len;
	ULONG       post_recv_times;
	CHAR        buffer[MAX_BUF_LEN];
	WSABUF      w_buf;
	OVERLAPPED	overlapped;
	// ...
} PER_IO_INFO, * PPER_IO_INFO;


typedef struct _PER_LINK_INFO
{
	LIST_ENTRY		list_entry;
	ULONG			free_flag;
	ULONG			state_machine;
	SOCKET			socket;
	PPER_IO_INFO	p_per_io_info;
	// ...
} PER_LINK_INFO, * PPER_LINK_INFO;


typedef struct _PACKET_HEADER
{
	ULONG comm_code;
	ULONG packet_len;
	// ...
} PACKET_HEADER, * PPACKET_HEADER;


typedef struct _PACKET_LOGON
{
	PACKET_HEADER packet_header;
	CHAR account[128];
	// ...
} PACKET_LOGON, * PPACKET_LOGON;


typedef struct _PACKET_GEO_LOCATION
{
	PACKET_HEADER packet_header;
	double latitude;
	double longitude;
	// ...
}PACKET_GEO_LOCATION, * PPACKET_GEO_LOCATION;


typedef struct _PACKET_EVENT
{
	PACKET_HEADER packet_header;
	CHAR type[8];
	LONG data;
}PACKET_EVENT, * PPACKET_EVENT;


INT random_number(INT min_num, INT max_num)
{
	INT low_num = 0, hi_num = 0;

	if (min_num < max_num)
	{
		low_num = min_num;
		hi_num = max_num + 1;
	}
	else
	{
		low_num = max_num + 1;
		hi_num = min_num;
	}

	srand(time(NULL));
	return (rand() % (hi_num - low_num)) + low_num;
}


double random_number(double min_num, double max_num)
{
	double low_num = 0.0, hi_num = 0.0;

	if (min_num < max_num)
	{
		low_num = min_num;
		hi_num = max_num + 1.0;
	}
	else
	{
		low_num = max_num + 1.0;
		hi_num = min_num;
	}

	srand(time(NULL));
	return low_num + (hi_num - low_num) * fabs(sin(rand() * rand()));
}

#endif