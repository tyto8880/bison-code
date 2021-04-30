import socket
import json
import random
import time
from datetime import datetime

# average is 6
sequenceLen = 5
# set threshold to 3
locations = ["40.24 55.30 200", "42.24 56.30 200", "30.24 65.30 200", "20.24 75.30 200"]

# for i in range(0, 25):
for i in range(len(locations)):
    s = socket.socket()
    s.connect(('127.0.0.1', 8000))

    now = datetime.now().isoformat()
    dummy = {
        'DeviceID': random.randint(0, 100000),
        'Timestamp': now,
        'EventID': i,
        'EventData': locations[i]
        }
    print(dummy)

    s.send(json.dumps(dummy).encode())

    s.close()
    time.sleep(1)
