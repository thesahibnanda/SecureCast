class UtilExceptions:
    
    class MailException(Exception):
        def __init__(self, message):
            self.message = message

        def __str__(self):
            return self.message

        def __repr__(self):
            return self.message
    
    class OTPException(Exception):
        def __init__(self, message):
            self.message = message

        def __str__(self):
            return self.message

        def __repr__(self):
            return self.message
    
    class HashGenerationException(Exception):
        def __init__(self, message):
            self.message = message

        def __str__(self):
            return self.message

        def __repr__(self):
            return self.message
        
    class HashValidationException(Exception):
        def __init__(self, message):
            self.message = message

        def __str__(self):
            return self.message

        def __repr__(self):
            return self.message