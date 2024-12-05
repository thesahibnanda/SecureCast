# Builder stage
FROM rust:1.72 AS builder

WORKDIR /app

# Copy source code
COPY . .

# Build the project and verify binary exists
RUN cargo build --release && ls -la /app/target/release

# Set environment variables for the application
ENV RUST_LOG=info

# Command to run the application
CMD ["./target/release/SecureCast"]
