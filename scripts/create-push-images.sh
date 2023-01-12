echo "Build and push bonusService"
docker build -t yuromanov/bonus-service:latest ../bonusService/.
docker push yuromanov/bonus-service

echo "Build and push flightService"
docker build -t yuromanov/flight-service:latest ../flightService/.
docker push yuromanov/flight-service

echo "Build and push ticketService"
docker build -t yuromanov/ticket-service:latest ../ticketService/.
docker push yuromanov/ticket-service

echo "Build and push gateway"
docker build -t yuromanov/gateway:latest ../gateway/.
docker push yuromanov/gateway