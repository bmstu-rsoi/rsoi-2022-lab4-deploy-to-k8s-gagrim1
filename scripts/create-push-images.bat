echo "Build and push bonusService"
docker build -t romanov/loyaltyapp:latest ../bonusService/.
docker push romanov/bonus-service

echo "Build and push flightService"
docker build -t romanov/paymentapp:latest ../flightService/.
docker push romanov/flight-service

echo "Build and push ticketService"
docker build -t romanov/reservationapp:latest ../ticketService/.
docker push romanov/ticket-service

echo "Build and push gateway"
docker build -t romanov/gateway:latest ../gateway/.
docker push romanov/gateway