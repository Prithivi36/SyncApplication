package Booking.Server2.server2.Service;

import Booking.Server2.server2.Entity.Booking;
import Booking.Server2.server2.Entity.BookingDto;
import Booking.Server2.server2.Entity.OrderDto;
import Booking.Server2.server2.Entity.OrderEntity;
import Booking.Server2.server2.Repository.BookingRepository;
import Booking.Server2.server2.Repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderEntryService {
    BookingRepository bookingRepository;
    RestTemplate restTemplate;
    ModelMapper mapper;
    OrderRepository orderRepository;

    public String addBookingNo() throws JsonProcessingException {
        String baseUrl="http://SpringServer1:8080";
        String exchangeValue=restTemplate.exchange(baseUrl+"/getall", HttpMethod.GET,new ResponseEntity<>(HttpStatus.OK),String.class).getBody();

        ObjectMapper objMapper = new ObjectMapper();
        objMapper.registerModule(new JavaTimeModule());

        List<Booking>  exchangedDto=objMapper.readValue(exchangeValue,new TypeReference<List<Booking>>(){});

        for(Booking bookingDto:exchangedDto){
            if(!bookingDto.isAwbSync()){
                OrderDto orderDto=new OrderDto();
                orderDto.setAwbno(bookingDto.getBookCnno());
                orderRepository.save(mapper.map(orderDto, OrderEntity.class));
                bookingRepository.save(bookingDto);
                restTemplate.put(baseUrl+"/change/"+bookingDto.getBookCnno(),bookingDto,HttpMethod.PUT);
            }
        }
        return exchangedDto.toString();
    }

    public String addBookingPieces() throws JsonProcessingException {

        List<OrderEntity> orderEntityList=orderRepository.findAll();

        for(OrderEntity orderEntity:orderEntityList){
            Long awbno=orderEntity.getAwbno();
            String baseUrl="http://SpringServer1:8080";
            String exchangeValue=restTemplate.exchange(baseUrl+"/bcn/"+awbno, HttpMethod.GET,new ResponseEntity<>(HttpStatus.OK),String.class).getBody();

            ObjectMapper objMapper = new ObjectMapper();
            objMapper.registerModule(new JavaTimeModule());

            Booking  exchangedDto=objMapper.readValue(exchangeValue,new TypeReference<Booking>(){});

            int pieces =exchangedDto.getBookPieces();

            if(pieces>0){
                orderEntity.setPieces(pieces);
                orderRepository.save(orderEntity);
                restTemplate.put(baseUrl+"/bcn/syn/"+awbno,exchangedDto,HttpMethod.PUT);

            }

        }
        return "Success";
    }
}
