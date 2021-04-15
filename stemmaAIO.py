
from board import SCL, SDA
from adafruit_seesaw.seesaw import Seesaw
from datetime import datetime
from Adafruit_IO import *

i2c_bus = busio.I2C(SCL,SDA)
stemma = Seesaw(i2c_bus, addr=0x36)
schedule = sched.scheduler(time.time, time.sleep)

# Learn how to set up your Adafruit IO account here: https://learn.adafruit.com/welcome-to-adafruit-io
#-------Adafruit IO-------#
aio = Client('AIO Username Here', 'AIO Key here')
mFeed = aio.feeds('stemma.moisture')
tFeed = aio.feeds('stemma.temperature')
#-------------------------#


# Average 100 samples of moisture
def moistureAvg():
    total = 0
    for i in range(100):
        total += stemma.moisture_read()
    return ("{:0.0f}".format(total / 100))


# Read soil data and send to Adafruit IO
def readSoilAdaIO():
    mTotal = moistureAvg()
    aio.send_data(mFeed.key, mTotal)

    temperature = ("{:0.0f}".format(stemma.get_temp()))
    aio.send_data(tFeed.key, temperature)


readSoilAdaIO()
quit()
