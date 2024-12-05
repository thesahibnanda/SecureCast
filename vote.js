/* Constants */
const IQAPI = "https://securecast-iqueue.onrender.com";
const CTAPI = "https://securecast-captaintree.onrender.com";
const DROTP_API = "https://securecast-p8oo.onrender.com";
const REDIRECT_URL = "https://www.google.com";

let userDetail = null;

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

async function ironMatchService(base64image1, base64image2) {
  const apiKeys = [
    "2fdec35c0dmshe880f09a7dc97b4p10cf2bjsn599519ad7811",
    "072df3bfcamsh45a882a659ddfc5p1e5fe8jsn6efb6b4c32c9",
    "93c85ba158msh90bea27485e920dp1f8611jsnbb0cb0c2f9c9",
    "b388ed232bmsh404fcb2561e0578p15676ajsn472e1f4876be",
    "0ce7b8cf17msh8b97d428f822cd5p18a1dcjsncfaa9f45cea1",
    "1831bbff3emsh0785fbb1865b13dp19527fjsn5518b119abf5",
  ];
  const randomApiKey = apiKeys[Math.floor(Math.random() * apiKeys.length)];

  const url = "https://facematch.p.rapidapi.com/API/verify/Facematch";
  const options = {
    method: "POST",
    headers: {
      "x-rapidapi-key": randomApiKey,
      "x-rapidapi-host": "facematch.p.rapidapi.com",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      method: "facevalidate",
      txn_id: `test-${Date.now()}`,
      clientid: "222",
      image_base64_1: base64image1,
      image_base64_2: base64image2,
    }),
  };

  try {
    const response = await fetch(url, options);

    if (!response.ok) {
      console.error("API request failed with status:", response.status);
      return false;
    }

    const result = await response.json();
    const confidence = parseFloat(result.Succeeded.data.confidence);
    console.log(confidence);
    if (confidence > 75) {
      return true;
    }
  } catch (error) {
    console.error("Error during API request:", error);
  }

  return false;
}

document.addEventListener("DOMContentLoaded", () => {
  const emailInput = document.getElementById("identifier");
  const otpButton = document.getElementById("get-otp");
  const otpInput = document.getElementById("otp");
  const submitButton = document.getElementById("submit-vote");
  const faceCaptureButton = document.getElementById("capture-face");
  const partyList = document.getElementById("party-list");

  const parties = [
    {
      partyName: "Bharatiya Janata Party",
      partyImage:
        "https://upload.wikimedia.org/wikipedia/commons/1/1e/Bharatiya_Janata_Party_logo.svg",
      partyLeader: "Narendra Modi",
    },
    {
      partyName: "Indian National Congress",
      partyImage:
        "https://upload.wikimedia.org/wikipedia/commons/6/63/Indian_National_Congress_hand_logo.png",
      partyLeader: "Rahul Gandhi",
    },
    {
      partyName: "Aam Aadmi Party",
      partyImage:
        "https://upload.wikimedia.org/wikipedia/commons/8/88/Aam_Aadmi_Party_%28AAP%29_Logo_New.png",
      partyLeader: "Arvind Kejriwal",
    },
  ];

  let selectedParty = null;
  let capturedFaceData = null;
  let hashedOtp = null;
  let otpTimestamp = null;

  // Enable or Disable Submit Button
  const validateForm = () => {
    submitButton.disabled = !(
      emailInput.value.trim() &&
      otpInput.value.trim() &&
      selectedParty &&
      capturedFaceData
    );
  };

  // Render Party List
  parties.forEach((party, index) => {
    const partyCard = document.createElement("div");
    partyCard.classList.add("party-card");
    partyCard.dataset.index = index;
    partyCard.innerHTML = `
            <img src="${party.partyImage}" alt="${party.partyName}">
            <h3>${party.partyName}</h3>
            <p>Leader: ${party.partyLeader}</p>
        `;
    partyCard.addEventListener("click", () => {
      document
        .querySelectorAll(".party-card")
        .forEach((card) => card.classList.remove("selected"));
      partyCard.classList.add("selected");
      selectedParty = party.partyName;
      alert(`You selected ${party.partyName}`);
      validateForm(); // Revalidate form when a party is selected
    });
    partyList.appendChild(partyCard);
  });

  // Enable OTP Button
  emailInput.addEventListener("input", () => {
    otpButton.disabled = !emailInput.value.trim();
    validateForm(); // Revalidate form on email input
  });

  // Fetch user details and handle OTP sending
  otpButton.addEventListener("click", async () => {
    try {
      const userResponse = await fetchWithTimeout(`${CTAPI}/user/details`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ identifier: emailInput.value }),
      });

      userDetail = userResponse;

      if (userDetail.error) {
        alert("User not found. Please register first.");
        window.location.href = "./reg.html";
        return;
      }

      // Send OTP to user's email
      const otpData = await fetchWithTimeout(`${DROTP_API}/init-otp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: userDetail.user.email }),
      });

      hashedOtp = otpData.otp;
      otpTimestamp = otpData.time;

      alert("OTP sent successfully! Please check your email.");
    } catch (error) {
      console.error("Error during OTP generation:", error);
      alert("An error occurred while sending the OTP. Please try again.");
    }
  });

  // Enable Submit Button on OTP Input
  otpInput.addEventListener("input", validateForm);

  // Face Capture Logic
  faceCaptureButton.addEventListener("click", async () => {
    const video = document.createElement("video");
    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d");
    canvas.width = 320;
    canvas.height = 240;

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      video.srcObject = stream;
      video.play();

      const modal = document.createElement("div");
      modal.style.display = "flex";
      modal.style.flexDirection = "column";
      modal.style.alignItems = "center";
      modal.style.justifyContent = "center";
      modal.style.background = "rgba(0, 0, 0, 0.8)";
      modal.style.position = "fixed";
      modal.style.top = 0;
      modal.style.left = 0;
      modal.style.width = "100%";
      modal.style.height = "100%";
      modal.style.zIndex = 9999;

      const captureButton = document.createElement("button");
      captureButton.textContent = "Capture Photo";
      captureButton.style.marginTop = "20px";
      captureButton.style.padding = "20px 20px";
      captureButton.style.width = "45%";
      captureButton.style.backgroundColor = "#007bff";
      captureButton.style.cursor = "pointer";
      modal.appendChild(video);
      modal.appendChild(captureButton);
      document.body.appendChild(modal);

      captureButton.addEventListener("click", () => {
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        capturedFaceData = canvas.toDataURL("image/png");

        // Clean up modal and stop the video stream
        stream.getTracks().forEach((track) => track.stop());
        document.body.removeChild(modal);

        faceCaptureButton.textContent = "Face Captured";
        faceCaptureButton.style.backgroundColor = "#28a745"; // Success green
        alert("Face captured successfully.");
        validateForm(); // Revalidate form when face is captured
      });
    } catch (error) {
      alert("Camera access denied or unavailable.");
      console.error("Error accessing camera:", error);
    }
  });

  // Submit Button Logic
  submitButton.addEventListener("click", async (event) => {
    event.preventDefault();

    if (!selectedParty || !capturedFaceData) {
      alert("Please select a party and capture your face before submitting.");
      return;
    }

    try {
      const otpValidationResponse = await fetchWithTimeout(
        `${DROTP_API}/validate-otp`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            otp: otpInput.value.trim(),
            hashOTP: hashedOtp,
            setTime: otpTimestamp,
          }),
        }
      );

      if (!otpValidationResponse.valid) {
        alert("Invalid OTP. Please try again.");
        return window.location.reload();
      }

      const faceMatchResult = await ironMatchService(
        capturedFaceData,
        userDetail.user.faceId
      );

      if (!faceMatchResult) {
        alert("Face verification failed. Please try again.");
        return window.location.reload();
      }

      const treeIntegrity = await fetchWithTimeout(`${CTAPI}/tree/verify`, {
        method: "GET",
      });

      if (!treeIntegrity.integrity) {
        alert("Please try again.");
        window.location.href = "./index.html";
        return;
      }
      const voteResponse = await fetchWithTimeout(`${CTAPI}/user/vote`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: userDetail.user.email,
          party: selectedParty,
        }),
      });

      if (voteResponse.error) {
        alert("Your vote is already casted.");
        return window.location.reload();
      }
      alert("Vote casted successfully!");
      window.location.href = "./ty.html";
      return;
    } catch (error) {
      console.error("Error during OTP validation:", error);
      alert(
        "An error occurred while validating OTP. Please try again." + error
      );
    }
  });
});
