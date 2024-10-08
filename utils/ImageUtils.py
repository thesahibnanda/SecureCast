import cv2
import numpy as np
import logging
import base64
from typing import Optional

class ImageUtils:
    _face_cascade: Optional[cv2.CascadeClassifier] = None

    @classmethod
    def initializeFaceCascade(cls, cascade_path: Optional[str] = None) -> None:
        """Initializes the face cascade classifier using a default path or a provided one."""
        if cascade_path:
            cls._face_cascade = cv2.CascadeClassifier(cascade_path)
        else:
            cls._face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        
        if cls._face_cascade.empty():
            logging.error("Failed to load face cascade classifier.")
            raise ValueError("Face cascade classifier could not be loaded.")
        logging.info('Face cascade classifier initialized')

    @staticmethod
    def base64ToImage(data: str) -> np.ndarray:
        """Convert base64 image to numpy array."""
        logging.info('Converting base64 image to numpy array')
        try:
            if 'base64,' in data:
                data = data.split('base64,')[1].strip()
            img_data = base64.b64decode(data)
            nparr = np.frombuffer(img_data, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            if img is None:
                logging.error('Image is None after decoding base64')
                raise ValueError('Image is None after decoding base64')
            return img
        except Exception as e:
            logging.error(f"Exception during base64 to image conversion: {str(e)}")
            raise ValueError("Failed to convert base64 to image") from e

    @classmethod
    def getFace(cls, image: np.ndarray) -> np.ndarray:
        """Detects the face in the given image and returns the cropped face as a NumPy array."""
        if cls._face_cascade is None:
            logging.error('Face cascade classifier not initialized')
            raise ValueError('Face cascade classifier is not initialized. Call initializeFaceCascade first.')
        
        logging.info('Detecting face in the image')
        try:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            faces = cls._face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))
            if len(faces) > 0:
                (x, y, w, h) = faces[0] 
                face = image[y:y+h, x:x+w]
                logging.info(f"Detected face with dimensions: x={x}, y={y}, w={w}, h={h}")
                return face
            else:
                logging.error('No face detected in the image')
                raise ValueError('No face detected')
        except Exception as e:
            logging.error(f"Error during face detection: {str(e)}")
            raise ValueError("Failed to detect face") from e
