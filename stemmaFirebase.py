import sched, time
import pyrebase
import busio
import sys

from firebasedata import LiveData
from board import SCL, SDA
from adafruit_seesaw.seesaw import Seesaw
from datetime import datetime

# Sleep to prevent IO error during crontab run
time.sleep(5)

i2c_bus = busio.I2C(SCL,SDA)
stemma = Seesaw(i2c_bus, addr=0x36)
schedule = sched.scheduler(time.time, time.sleep)

# Learn how to set up a Firebase Realtime Database Here: https://firebase.google.com/
#-------Pyrebase-------#
pyrebase_config = {
    "apiKey": "Firebase API Key Here",
    "authDomain": "Project Domain Here",
    "databaseURL": "Realtime Database Here",
    "storageBucket": "Storage Bucket URL Here"
}

app = pyrebase.initialize_app(pyrebase_config)
live = LiveData(app, '/my_data')

# Get a snapshot of all data at the path
db = app.database()
#----------------------#


# Average 100 samples of moisture
def moistureAvg():
    total = 0
    for i in range(100):
        total += stemma.moisture_read()
    return ("{:0.0f}".format(total / 100))


# Read soil data and send to Firebase
def readSoilFireBase(name):
    mTotal = moistureAvg()

    moisture = (mTotal)
    temp = ("{:0.0f}".format(stemma.get_temp()))

    now = datetime.now()
    year = now.strftime("%Y")
    month = now.strftime("%m")
    day = now.strftime("%d")
    hour = now.strftime("%H")
    minute = now.strftime("%M")

    # Unique string from date and time
    readingString = year + month + day + hour + minute

    # JSON string for child data
    data = {
        "date": {
            "year": year,
            "month": month,
            "day": day,
            "hour": hour,
            "minute": minute
        },
        "soil": {
            "moisture": moisture,
            "temperature": temp
        },
        "name": name
    }

    # Add reading to database
    db.child(name).child(readingString).set(data)


readSoilFireBase("stemma_1")
quit()
