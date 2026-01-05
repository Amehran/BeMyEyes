
# Use an official lightweight Python image.
# 3.11-slim is small and secure.
FROM python:3.11-slim

# Set environment variables to prevent Python from buffering stdout/stderr
# (useful for seeing logs in Cloud Run immediately)
ENV PYTHONUNBUFFERED True

# Set the working directory in the container
WORKDIR /app

# Copy the requirements file explicitly first (for layer caching optimization)
COPY backend/requirements.txt .

# Install dependencies
# --no-cache-dir reduces image size
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the backend code
COPY backend/ .

# Expose the port that Uvicorn will listen on (default for Cloud Run is 8080)
ENV PORT 8080

# Command to run the application using Uvicorn
# "0.0.0.0" allows external access (required for Docker/Cloud Run)
CMD uvicorn app.main:app --host 0.0.0.0 --port 8080
