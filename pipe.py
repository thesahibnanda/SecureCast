import cv2

from utils.ImageUtils import ImageUtils
from models.FaceModel import compareFaces

def pipeline(image1: str, image2: str, threshold: float = 0.6, model_name: str = "ArcFace", backend: str = "retinaface") -> bool:
    """Pipeline to compare two images of faces."""
    ImageUtils.initializeFaceCascade(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    img1 = ImageUtils.base64ToImage(image1)
    img2 = ImageUtils.base64ToImage(image2)
    img1 = ImageUtils.getFace(img1)
    img2 = ImageUtils.getFace(img2)
    return compareFaces(img1, img2, threshold, model_name, backend)