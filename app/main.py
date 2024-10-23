from confluent_kafka import Consumer

c = Consumer({
    # 'bootstrap.servers': 'my-cluster-kafka-bootstrap:9092',
    'bootstrap.servers': 'localhost:9092',
    'group.id': 'embeddings',
    'auto.offset.reset': 'earliest'
})

c.subscribe(['embeddings'])

while True:
    msg = c.poll(1.0)

    if msg is None:
        continue
    if msg.error():
        print("Consumer error: {}".format(msg.error()))
        continue

    print('Received message: {}'.format(msg.value().decode('utf-8')))

c.close()