package com.romanov.gateway.service;

import com.romanov.gateway.config.AppParams;
import com.romanov.gateway.exception.BonusServiceNotAvailableException;
import com.romanov.gateway.exception.FlightServiceNotAvailableException;
import com.romanov.gateway.exception.GatewayErrorException;
import com.romanov.gateway.exception.TicketServiceNotAvailableException;
import com.romanov.gateway.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GatewayServiceImpl implements GatewayService {
    private static final String USERNAME_PARAM = "X-User-Name";
    private final AppParams params;
    @Resource
    private WebClient webClient;

    @Override
    public ResponseEntity<?> getFlights(Integer page, Integer size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostFlight())
                        .path(params.getPathFlight() + "/all")
                        .port(params.getPortFlight())
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new FlightServiceNotAvailableException(error.statusCode());
                })
                .toEntity(Object.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    @Override
    public ResponseEntity<?> getPrivilegeWithHistory(String username) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostBonus())
                        .path(params.getPathBonus() + "/with-history")
                        .port(params.getPortBonus())
                        .build())
                .header(USERNAME_PARAM, username)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new BonusServiceNotAvailableException(error.statusCode());
                })
                .toEntity(Object.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    @Override
    public UserInfoOutput getUserInfo(String username) {
        return new UserInfoOutput(getFullTickets(username), getPrivilege(username));
    }

    private List<FullTicketOutput> getFullTickets(String username) {
        List<FullTicketOutput> fullTickets = new ArrayList<>();
        List<TicketOutput> tickets = getTickets(username);
        tickets.forEach(value -> {
            FlightOutput flight = getFlight(value.getFlightNumber());
            fullTickets.add(
                    new FullTicketOutput(value.getTicketUid(),
                            value.getFlightNumber(),
                            flight.getFromAirport(),
                            flight.getToAirport(),
                            flight.getDate(),
                            value.getPrice(),
                            value.getStatus())
            );
        });
        return fullTickets;
    }

    private PrivilegeOutput getPrivilege(String username) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostBonus())
                        .path(params.getPathBonus())
                        .port(params.getPortBonus())
                        .build())
                .header(USERNAME_PARAM, username)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new BonusServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(PrivilegeOutput.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    @Override
    public List<FullTicketOutput> getUserTickets(String username) {
        List<FullTicketOutput> fullTickets = new ArrayList<>();
        List<TicketOutput> tickets = getTickets(username);
        tickets.forEach(value -> {
            FlightOutput flight = getFlight(value.getFlightNumber());
            fullTickets.add(
                    new FullTicketOutput(value.getTicketUid(),
                            value.getFlightNumber(),
                            flight.getFromAirport(),
                            flight.getToAirport(),
                            flight.getDate(),
                            value.getPrice(),
                            value.getStatus())
            );
        });
        return fullTickets;
    }

    private List<TicketOutput> getTickets(String username) {
        TicketOutput[] tickets = getArrayOfTickets(username);
        if (tickets != null) {
            return Arrays.asList(tickets);
        } else {
            throw new RuntimeException("");
        }
    }

    private TicketOutput[] getArrayOfTickets(String username) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostTicket())
                        .path(params.getPathTicket())
                        .port(params.getPortTicket())
                        .build()
                )
                .header(USERNAME_PARAM, username)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new TicketServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(TicketOutput[].class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    @Override
    public FullTicketOutput getTicketByUid(String username, UUID ticketUid) {
        TicketOutput ticketOutput = getTicket(username, ticketUid);
        FlightOutput flightOutput = getFlight(ticketOutput.getFlightNumber());
        return new FullTicketOutput(ticketOutput.getTicketUid(),
                ticketOutput.getFlightNumber(),
                flightOutput.getFromAirport(),
                flightOutput.getToAirport(),
                flightOutput.getDate(),
                ticketOutput.getPrice(),
                ticketOutput.getStatus());
    }

    private TicketOutput getTicket(String username, UUID ticketUid) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostTicket())
                        .path(params.getPathTicket() + "/" + ticketUid)
                        .port(params.getPortTicket())
                        .build()
                )
                .header(USERNAME_PARAM, username)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new TicketServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(TicketOutput.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    @Override
    public BuyingOutput buyTicket(String username, BuyingInput input) {
        TicketOutput ticket = createTicket(username, input);
        FlightOutput flightOutput = getFlight(ticket.getFlightNumber());
        CalculationPriceInput priceInput = new CalculationPriceInput(
                username,
                ticket.getTicketUid(),
                input.getPrice(),
                input.getPaidFromBalance()
        );
        BonusOutput bonus = calculatePrice(priceInput);
        return BuyingOutput.builder()
                .ticketUid(ticket.getTicketUid())
                .flightNumber(ticket.getFlightNumber())
                .fromAirport(flightOutput.getFromAirport())
                .toAirport(flightOutput.getToAirport())
                .date(flightOutput.getDate())
                .price(ticket.getPrice())
                .paidByMoney(bonus.getPaidByMoney())
                .paidByBonuses(bonus.getPaidByBonuses())
                .status(ticket.getStatus())
                .privilege(bonus.getPrivilege())
                .build();
    }

    private TicketOutput createTicket(String username, BuyingInput input) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostTicket())
                        .path(params.getPathTicket())
                        .port(params.getPortTicket())
                        .build())
                .header(USERNAME_PARAM, username)
                .body(BodyInserters.fromValue(input))
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new TicketServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(TicketOutput.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    private FlightOutput getFlight(String flightNumber) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostFlight())
                        .path(params.getPathFlight())
                        .port(params.getPortFlight())
                        .queryParam("flightNumber", flightNumber)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new FlightServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(FlightOutput.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    private BonusOutput calculatePrice(CalculationPriceInput input) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostBonus())
                        .path(params.getPathBonus() + "/calculate")
                        .port(params.getPortBonus())
                        .build())
                .body(BodyInserters.fromValue(input))
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new BonusServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(BonusOutput.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    @Override
    public void returnTicket(String username, UUID ticketUid) {
        cancelTicket(username, ticketUid);
        returnBonuses(username, ticketUid);
    }

    private void returnBonuses(String username, UUID ticketUid) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostBonus())
                        .path(params.getPathBonus() + "/" + ticketUid)
                        .port(params.getPortBonus())
                        .build())
                .header(USERNAME_PARAM, username)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new BonusServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(Void.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }

    private void cancelTicket(String username, UUID ticketUid) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .host(params.getHostTicket())
                        .path(params.getPathTicket() + "/" + ticketUid)
                        .port(params.getPortTicket())
                        .build())
                .header(USERNAME_PARAM, username)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    throw new TicketServiceNotAvailableException(error.statusCode());
                })
                .bodyToMono(Void.class)
                .onErrorMap(Throwable.class, error -> {
                    throw new GatewayErrorException(error.getMessage());
                })
                .block();
    }
}
