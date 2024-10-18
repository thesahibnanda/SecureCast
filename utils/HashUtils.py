import hashlib
import logging  

from utils.UtilExceptions import UtilExceptions

class HashUtils:
    @staticmethod
    def hash_string(string: str) -> str:
        try:
            return hashlib.sha256(hashlib.sha256(string.encode()).digest()).hexdigest()
        except Exception as e:
            logging.error("Error while hashing string: " + str(e))
            raise UtilExceptions.HashGenerationException(str(e))
    
    @staticmethod
    def validate_hash(string: str, hash: str) -> bool:
        try:
            return HashUtils.hash_string(string) == hash
        except Exception as e:
            logging.error("Error while validating hash: " + str(e))
            raise UtilExceptions.HashValidationException(str(e))