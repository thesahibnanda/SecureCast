# Use Python 3.12.4 as the base image
FROM python:3.12.4-slim

# Set the working directory inside the container
WORKDIR /app

# Install dependencies for OpenCV, including libGL
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0

# Copy requirements.txt file into the container
COPY requirements.txt .

# Install the dependencies from requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application code into the container
COPY . .

# Expose port 8000 for the application
EXPOSE 8000

# Command to run the FastAPI application using Gunicorn and Uvicorn workers
CMD ["gunicorn", "-w", "4", "-k", "uvicorn.workers.UvicornWorker", "app:app", "--bind", "0.0.0.0:8000", "--timeout", "300", "--threads", "8"]
