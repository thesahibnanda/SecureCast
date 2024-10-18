import os
import json
import dotenv 

dotenv.load_dotenv()

class Config:
    SENDER_EMAIL = os.getenv('SENDER_EMAIL')
    SENDER_PASSWORD = json.loads(os.getenv('SENDER_PASSWORD'))[0]
    SUBJECT = os.getenv('SUBJECT')
    MESSAGE = os.getenv('MESSAGE')
    SECRET_KEY = os.getenv('YOUR_SECRET_KEY')
    MAX_AGE = int(os.getenv('MAX_AGE'))