import secrets
import logging

from utils.UtilExceptions import UtilExceptions

class OTPUtils:
    @staticmethod
    def generate_otp(length: int = 6) -> str:
        try:
            return ''.join([str(secrets.choice(range(10))) for _ in range(length)])
        except Exception as e:
            logging.error("Error while generating OTP: " + str(e))
            raise UtilExceptions.OTPGenerationException(str(e))