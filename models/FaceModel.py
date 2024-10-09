import os
import cv2
import uuid
import time
import logging
import numpy as np
from threading import Lock
from deepface import DeepFace



logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - [%(filename)s:%(funcName)s] - %(message)s',
    level=logging.DEBUG 
)

lock = Lock()

def compareFaces(image1: np.ndarray, image2: np.ndarray, threshold: float, backend: str) -> bool:
    """
    Compares two images of faces (passed as NumPy arrays) and returns True if they match, False otherwise.
    The function saves the images as temporary files with unique names (timestamp + uuid), verifies using DeepFace,
    and deletes the files afterward. It handles concurrent requests safely.
    
    Parameters:
        image1 (np.ndarray): First image as a NumPy array.
        image2 (np.ndarray): Second image as a NumPy array.
        threshold (float): Threshold for determining a match.
        modelName (str): Name of the model to use for face recognition (default is 'ArcFace').
        backend (str): Backend to use ('tensorflow', 'pytorch', etc.). Default is 'tensorflow'.
    
    Returns:
        bool: True if faces match, False otherwise.
    """

    def saveTempImage(image: np.ndarray) -> str:
        """
        Saves a NumPy array image to a temporary file and returns the file path.
        The file name is generated based on a timestamp and a UUID to ensure uniqueness.
        
        Parameters:
            image (np.ndarray): Image to save.
        
        Returns:
            str: Path to the temporary image file.
        """
        timestamp = int(time.time())
        uniqueId = uuid.uuid4()
        tempFileName = f"temp_{timestamp}_{uniqueId}.jpg"
        os.makedirs("temp", exist_ok=True)
        tempFilePath = os.path.join(os.getcwd(), "temp", tempFileName)
        cv2.imwrite(tempFilePath, image)
        return tempFilePath

    def cleanUp(files):
        """
        Deletes the given list of file paths to clean up temporary files.
        
        Parameters:
            files (list of str): List of file paths to delete.
        """
        for filePath in files:
            try:
                if os.path.exists(filePath):
                    os.remove(filePath)
                    logging.info(f"Deleted temporary file: {filePath}")
            except Exception as e:
                logging.error(f"Error deleting temporary file {filePath}: {str(e)}")

    def verifyImages(img1Path: str, img2Path: str) -> bool:
        """
        Verifies the two images using DeepFace and returns True if they match.
        
        Parameters:
            img1Path (str): Path to the first image file.
            img2Path (str): Path to the second image file.
        
        Returns:
            bool: True if the images match based on the threshold.
        """
        try:
            result = DeepFace.verify(img1_path=img1Path, img2_path=img2Path, detector_backend=backend)
            logging.info(f"Face comparison result: {result}")
            return result['distance'] < threshold
        except Exception as e:
            logging.error(f"Exception during face comparison: {str(e)}")
            raise ValueError("Failed to compare faces") from e

    with lock:
        img1Path = saveTempImage(image1)
        img2Path = saveTempImage(image2)

        try:
            isMatch = verifyImages(img1Path, img2Path)
        finally:
            cleanUp([img1Path, img2Path])

        return isMatch
