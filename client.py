import socket

s = socket.socket()
s.connect(('127.0.0.1', 8000))

s.send(b'40.12071, -140.3291')

s.close()

s = socket.socket()
s.connect(('127.0.0.1', 8000))

s.send(b'400.12071, -140.3291')

s.close()

s = socket.socket()
s.connect(('127.0.0.1', 8000))

s.send(b'4000.12071, -140.3291')

s.close()