/* Constants */
const IQAPI = "https://securecast-iqueue.onrender.com";
const CTAPI = "https://securecast-captaintree.onrender.com";
const DROTP_API = "https://securecast-p8oo.onrender.com";
const REDIRECT_URL = "https://www.google.com";
const ITERATIONS = 10000;

/* Utility to fetch with timeout and retries */
async function fetchWithTimeout(
  url,
  options = {},
  timeout = 5000,
  retries = 3
) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);
  options.signal = controller.signal;

  try {
    const response = await fetch(url, options);
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP Error ${response.status}: ${errorText}`);
    }
    return await response.json();
  } catch (err) {
    if (retries > 0) {
      console.warn(`Retrying (${3 - retries + 1}):`, err.message);
      return fetchWithTimeout(url, options, timeout, retries - 1);
    }
    throw err;
  } finally {
    clearTimeout(timer);
  }
}

/* Check if user exists and return flag */
async function isUpdate(email) {
  try {
    const response = await fetchWithTimeout(`${CTAPI}/user/details`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identifier: email }),
    });
    return !response.error; // User exists if there's no error
  } catch (err) {
    console.error("Error checking user existence:", err);
    return false; // Default to new user if request fails
  }
}

/* Background Queue Processor */
async function processIQToCT(isUpdateFlag, batchSize = 5) {
  console.log("Starting background queue processor...");
  let iterationCount = 0;

  while (iterationCount < ITERATIONS) {
    iterationCount++;
    try {
      const queueMetrics = await fetchWithTimeout(`${IQAPI}/metrics`);
      if (queueMetrics.queue_size === 0) {
        console.log("IQ is empty. Waiting for new tasks...");
        await new Promise((resolve) => setTimeout(resolve, 5000));
        return;
      }

      const tasks = [];
      for (let i = 0; i < batchSize; i++) {
        tasks.push(
          fetchWithTimeout(`${IQAPI}/get-user`, { method: "GET" }).then(
            async (userResponse) => {
              if (userResponse?.is_returned_data) {
                const userData = JSON.parse(userResponse.data);
                const treeIntegrity = await fetchWithTimeout(
                  `${CTAPI}/tree/verify`,
                  { method: "GET" }
                );

                if (treeIntegrity.integrity) {
                  console.log("Tree Integrity is maintained");
                  const endpoint = isUpdateFlag
                    ? `${CTAPI}/user/update`
                    : `${CTAPI}/user/add`;
                  const method = isUpdateFlag ? "PUT" : "POST";

                  console.log(endpoint, method);

                  const ctResponse = await fetchWithTimeout(endpoint, {
                    method: method,
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(userData),
                  });

                  console.log(
                    `User ${isUpdateFlag ? "updated" : "added"} to CT: ${
                      ctResponse.message
                    }`
                  );
                } else {
                  console.error("CT tree integrity failed. Skipping user...");
                }
              }
            }
          )
        );
      }

      // Process batch of tasks
      await Promise.all(tasks);
    } catch (err) {
      console.error("Error in processing queue:", err);
      await new Promise((resolve) => setTimeout(resolve, 5000)); // Wait before retrying
    }
  }
}

let globalFlag = null;

/* Frontend Logic */
document.addEventListener("DOMContentLoaded", function () {
  const modal = document.getElementById("camera-modal");
  const uploadFaceButton = document.getElementById("upload-face");
  const closeModal = document.querySelector(".close");
  const video = document.getElementById("video");
  const canvas = document.getElementById("canvas");
  const captureButton = document.getElementById("capture-photo");
  const recaptureButton = document.getElementById("recapture-photo");
  const form = document.getElementById("signup-form");
  const otpButton = document.querySelector(".otp-button");
  const otpInput = document.querySelector(".otp-input");
  const emailInput = document.getElementById("email");
  const context = canvas.getContext("2d");
  let stream,
    hashed_otp = null,
    setTime = null;

  // Set up video and canvas dimensions
  video.width = 320;
  video.height = 240;
  canvas.width = 320;
  canvas.height = 240;

  // Enable OTP button when email is valid
  emailInput.addEventListener("input", function () {
    otpButton.disabled = !emailInput.validity.valid;
  });

  // Handle OTP button click
  otpButton.addEventListener("click", async function () {
    if (!emailInput.validity.valid) {
      alert("Please enter a valid email address.");
      return;
    }

    try {
      const response = await fetchWithTimeout(`${DROTP_API}/init-otp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: emailInput.value }),
      });
      hashed_otp = response.otp;
      setTime = response.time;
      alert("OTP sent successfully! Please check your email.");
    } catch (error) {
      console.error("Error sending OTP:", error);
      alert("An error occurred while sending the OTP.");
    }
  });

  // Open the modal and start the camera when "Upload Face" is clicked
  uploadFaceButton.addEventListener("click", function () {
    modal.style.display = "flex";
    video.style.display = "block";
    canvas.style.display = "none";
    captureButton.style.display = "inline-block";
    recaptureButton.style.display = "none";

    if (navigator.mediaDevices?.getUserMedia) {
      navigator.mediaDevices
        .getUserMedia({ video: true })
        .then((mediaStream) => {
          stream = mediaStream;
          video.srcObject = stream;
          video.play();
        })
        .catch((error) => {
          alert("Camera access denied or unavailable.");
          console.error("Error accessing the camera:", error);
        });
    } else {
      alert("Your browser does not support accessing the camera.");
    }
  });

  // Close the modal and stop the camera
  closeModal.addEventListener("click", function () {
    modal.style.display = "none";
    if (stream) {
      stream.getTracks().forEach((track) => track.stop());
    }
  });

  // Capture the photo
  captureButton.addEventListener("click", function () {
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    video.style.display = "none";
    canvas.style.display = "block";
    captureButton.style.display = "none";
    recaptureButton.style.display = "inline-block";

    uploadFaceButton.style.backgroundColor = "#28a745"; // Green color for success
    uploadFaceButton.innerHTML = "Face Captured";
    uploadFaceButton.setAttribute("data-captured", "true");
  });

  // Recapture the photo
  recaptureButton.addEventListener("click", function () {
    video.style.display = "block";
    canvas.style.display = "none";
    captureButton.style.display = "inline-block";
    recaptureButton.style.display = "none";

    uploadFaceButton.style.backgroundColor = "#555";
    uploadFaceButton.innerHTML = "Upload Face (Required)";
    uploadFaceButton.setAttribute("data-captured", "false");
  });

  // Handle form submission
  form.addEventListener("submit", async function (e) {
    e.preventDefault();

    if (uploadFaceButton.getAttribute("data-captured") === "false") {
      alert("Please capture a face image before proceeding.");
      return;
    }

    try {
      const otpResponse = await fetchWithTimeout(`${DROTP_API}/validate-otp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          otp: otpInput.value,
          hashOTP: hashed_otp,
          setTime: setTime,
        }),
      });

      if (!otpResponse.valid) {
        alert(`OTP Validation Failed: ${otpResponse.message}`);
        return window.location.reload();
      }

      const userData = {
        name: form.elements[0].value,
        email: form.elements[1].value,
        address: form.elements[3].value,
        aadharCardNumber: form.elements[2].value,
        faceId: canvas.toDataURL("image/png"),
      };

      await fetchWithTimeout(`${IQAPI}/add-user`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ data: JSON.stringify(userData) }),
      });

      const isUpdateFlag = await isUpdate(userData.email);
      globalFlag = isUpdateFlag;

      if (isUpdateFlag) {
        alert("Your Details will be updated");
      } else {
        alert("Your Details will be added");
      }

      await processIQToCT(globalFlag).catch((err) =>
        console.error("Error in background queue processor:", err)
      );

      window.location.href = "./ty-reg.html";
    } catch (error) {
      console.error("Error submitting form:", error);
      alert("An error occurred while submitting the form.");
    }
  });
});
