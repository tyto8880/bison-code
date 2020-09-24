import socket

s = socket.socket()

s.connect(('127.0.0.1', 8000))

s.send(b'40.12071, -140.3291')