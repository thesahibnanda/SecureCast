from fastapi import FastAPI, Cookie
from pydantic import BaseModel, EmailStr
from typing import Optional
from starlette.middleware.sessions import SessionMiddleware
from starlette.responses import JSONResponse
from datetime import datetime, timedelta

from config import Config
from utils.HashUtils import HashUtils
from utils.MailUtils import MailUtils
from utils.OTPUtils import OTPUtils

app = FastAPI()

app.add_middleware(SessionMiddleware, secret_key=Config.SECRET_KEY, max_age=Config.MAX_AGE)

class OTPRequest(BaseModel):
    email: EmailStr
    
class OTPValidationRequest(BaseModel):
    otp: str

@app.post("/init-otp")
async def init_otp(request: OTPRequest):
    try:
        otp = OTPUtils.generate_otp()
        hashed_otp = HashUtils.hash_string(otp)
        response  = JSONResponse(content={"error": False, "message": "OTP sent successfully"})
        MailUtils.send_mail(Config.SENDER_EMAIL, Config.SENDER_PASSWORD, request.email, Config.SUBJECT, Config.MESSAGE + otp)
        response.set_cookie(key="otp_hash", value=hashed_otp, max_age=Config.MAX_AGE, secure=True, httponly=True)
        return response
    except Exception as e:
        return JSONResponse(content={"error": True, "message": str(e)})

@app.post("/validate-otp")
async def validate_otp(request: OTPValidationRequest, otp_hash: Optional[str] = Cookie(None)):
    try:
        if not otp_hash:
            return JSONResponse(content={"error": True, "valid": False, "message": "OTP has expired or not set"})
        is_valid = HashUtils.validate_hash(request.otp, otp_hash)
        
        if is_valid:
            return JSONResponse(content={"error": False, "valid": True, "message": "OTP is valid"})
        else:
            return JSONResponse(content={"error": False, "valid": False, "message": "Invalid OTP"})
    except Exception as e:
        return JSONResponse(content={"error": True, "valid": False, "message": str(e)})