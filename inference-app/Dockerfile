FROM nvidia/cuda:11.8.0-cudnn8-devel-ubuntu22.04 AS builder

# Install Python 3.12
RUN apt-get update && apt-get install -y software-properties-common
RUN add-apt-repository ppa:deadsnakes/ppa
RUN apt-get update && apt-get install -y python3.12 python3.12-venv python3.12-dev python3-pip

# Set Python 3.12 as the default python
RUN update-alternatives --install /usr/bin/python python /usr/bin/python3.12 1
RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.12 1

# Install poetry
RUN pip3 install poetry==1.4.2

ENV POETRY_NO_INTERACTION=1 \
    POETRY_VIRTUALENVS_IN_PROJECT=1 \
    POETRY_VIRTUALENVS_CREATE=1 \
    POETRY_CACHE_DIR=/tmp/poetry_cache

WORKDIR /inference-app

COPY pyproject.toml poetry.lock ./
RUN touch README.md

RUN --mount=type=cache,target=$POETRY_CACHE_DIR poetry install --without dev --no-root

FROM nvidia/cuda:11.8.0-cudnn8-runtime-ubuntu22.04 AS runtime

# Install Python 3.12
RUN apt-get update && apt-get install -y software-properties-common
RUN add-apt-repository ppa:deadsnakes/ppa
RUN apt-get update && apt-get install -y python3.12 python3.12-venv
RUN update-alternatives --install /usr/bin/python python /usr/bin/python3.12 1
RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.12 1

ENV VIRTUAL_ENV=/inference-app/.venv \
    PATH="/inference-app/.venv/bin:$PATH"

COPY --from=builder ${VIRTUAL_ENV} ${VIRTUAL_ENV}

WORKDIR /inference-app

COPY src ./src
COPY schemas/ ./schemas/

ENTRYPOINT ["python", "src/main.py"]
