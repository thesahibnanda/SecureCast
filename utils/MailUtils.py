import smtplib
import logging
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from typing import Optional

from utils.UtilExceptions import UtilExceptions

class MailUtils:
    @staticmethod
    def send_mail(sender_mail: str, sender_password: str, receiver_mail: str, subject: str, body: str, smtp_server: str = "smtp.gmail.com", smtp_port: int = 587, use_tls: bool = True, use_ssl: bool = False, timeout: Optional[float] = 30.0):
        try:
            message = MIMEMultipart()
            message['From'] = sender_mail
            message['To'] = receiver_mail
            message['Subject'] = subject
            message.attach(MIMEText(body, 'plain'))
            if use_ssl:
                server = smtplib.SMTP_SSL(smtp_server, smtp_port, timeout=timeout)
            else:
                server = smtplib.SMTP(smtp_server, smtp_port, timeout=timeout)
            if use_tls and not use_ssl:
                server.starttls()
            server.login(sender_mail, sender_password)
            server.sendmail(sender_mail, receiver_mail, message.as_string())
        except smtplib.SMTPException as smtp_error:
            logging.error(f"SMTP error occurred: {smtp_error}")
            raise UtilExceptions.MailException(f"SMTP error occurred: {smtp_error}")
        except Exception as e:
            logging.error(f"Unexpected error while sending mail: {e}")
            raise UtilExceptions.MailException(f"Unexpected error while sending mail: {e}")
        finally:
            try:
                server.quit()
            except Exception as quit_error:
                logging.warning(f"Error while closing the SMTP connection: {quit_error}")