############################
# Stage 1: Build Python 3.12
############################
FROM nvidia/cuda:12.6.2-devel-ubuntu22.04 AS builder

# Set environment variables
ENV PYTHON_VERSION=3.12.0
ENV DEBIAN_FRONTEND=noninteractive

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    wget \
    libssl-dev \
    zlib1g-dev \
    libncurses5-dev \
    libncursesw5-dev \
    libreadline-dev \
    libsqlite3-dev \
    libgdbm-dev \
    libdb5.3-dev \
    libbz2-dev \
    libexpat1-dev \
    liblzma-dev \
    tk-dev \
    libffi-dev \
    uuid-dev \
    libnss3-dev \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Download and extract Python source code
RUN wget https://www.python.org/ftp/python/$PYTHON_VERSION/Python-$PYTHON_VERSION.tgz && \
    tar xzf Python-$PYTHON_VERSION.tgz && \
    rm Python-$PYTHON_VERSION.tgz

# Build and install Python
WORKDIR /Python-$PYTHON_VERSION
RUN ./configure --enable-optimizations --prefix=/usr/local && \
    make -j$(nproc) && \
    make altinstall

# Clean up build dependencies and Python source
RUN apt-get purge -y --auto-remove build-essential wget && \
    rm -rf /Python-$PYTHON_VERSION

############################
# Stage 2: Final image
############################
FROM nvidia/cuda:12.6.2-runtime-ubuntu22.04

# Copy Python 3.12 from the builder stage
COPY --from=builder /usr/local /usr/local

# Create symlink for python3.12
RUN ln -s /usr/local/bin/python3.12 /usr/local/bin/python

# Install runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libssl3 \
    libbz2-1.0 \
    libffi7 \
    liblzma5 \
    libsqlite3-0 \
    libnss3 \
    zlib1g \
    libgdbm6 \
    libreadline8 \
    libncursesw6 \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install Poetry
RUN python -m ensurepip --upgrade && \
    pip install --no-cache-dir poetry

# Set PATH for Poetry
ENV PATH="/root/.local/bin:$PATH"

# Set working directory
WORKDIR /inference-app

# Copy Poetry configuration files
COPY pyproject.toml poetry.lock ./

# Install project dependencies (without dev dependencies)
RUN poetry config virtualenvs.create false && \
    poetry install --no-dev --no-interaction --no-ansi

# Copy application source code
COPY src/ ./src/
COPY schemas/ ./schemas/

# Set environment variables for NVIDIA and CUDA (if necessary)
ENV NVIDIA_VISIBLE_DEVICES all
ENV NVIDIA_DRIVER_CAPABILITIES compute,utility
ENV LD_LIBRARY_PATH /usr/local/cuda/lib64:$LD_LIBRARY_PATH

# Set the entrypoint
CMD ["python", "src/main.py"]
