import time
from fastapi import FastAPI, Query
from pydantic import BaseModel, EmailStr
from starlette.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from config import Config
from utils.HashUtils import HashUtils
from utils.MailUtils import MailUtils
from utils.OTPUtils import OTPUtils

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"], 
    allow_headers=["*"],
)

class OTPRequest(BaseModel):
    email: EmailStr
    
class OTPValidationRequest(BaseModel):
    otp: str
    hashOTP: str
    setTime: float

@app.post("/init-otp")
async def init_otp(request: OTPRequest):
    
    try:
        otp = OTPUtils.generate_otp()
        hashed_otp = HashUtils.hash_string(otp)
        MailUtils.send_mail(Config.SENDER_EMAIL, Config.SENDER_PASSWORD, request.email, Config.SUBJECT, Config.MESSAGE + otp)
        
        return JSONResponse(content={"error": False, "message": "OTP sent successfully", "otp": hashed_otp, "time": time.time()})
    
    except Exception as e:
        return JSONResponse(content={"error": True, "message": str(e)})

@app.post("/validate-otp")
async def validate_otp(request: OTPValidationRequest):
    try:
        
        if request.hashOTP is None:
            return JSONResponse(content={"error": False, "valid": False, "message": "OTP is expired or was never generated"})
        
        if time.time() - request.setTime > Config.MAX_AGE:
            return JSONResponse(content={"error": False, "valid": False, "message": "OTP is expired"})
        
        if HashUtils.validate_hash(request.otp, request.hashOTP):
            return JSONResponse(content={"error": False, "valid": True, "message": "OTP is valid"})
        
        else:
            return JSONResponse(content={"error": False, "valid": False, "message": "Invalid OTP"})
        
    except Exception as e:
        return JSONResponse(content={"error": True, "valid": False, "message": str(e)})

@app.get("/healthz")
async def healthz():
    return JSONResponse(content={"status": "ok"}, status_code=200)