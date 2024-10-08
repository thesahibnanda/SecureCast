import logging
from typing import Dict
from fastapi import FastAPI
from pydantic import BaseModel

from pipe import pipeline


app = FastAPI()

class ImageRequest(BaseModel): 
    image1: str
    image2: str

    
    
    

@app.post("/face-match/")
async def face_match(req: ImageRequest) -> Dict[str, bool]:
    try:
        is_match = pipeline(req.image1, req.image2) 
        return {
            "is_error": False,
            "is_match": is_match
        }
    except Exception as e:
        logging.error(f"Error processing face comparison: {str(e)}")
        return {
            "is_error": True,
            "error_message": str(e),
            "is_match": None
        }