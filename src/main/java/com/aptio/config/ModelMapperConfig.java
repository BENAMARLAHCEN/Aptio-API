package com.aptio.config;

import com.aptio.dto.ScheduleEntryDTO;
import com.aptio.model.ScheduleEntry;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);
//        modelMapper.typeMap(ScheduleEntry.class, ScheduleEntryDTO.class).setPostConverter(context -> {
//            ScheduleEntry source = context.getSource();
//            ScheduleEntryDTO destination = context.getDestination();
//
//            if (source.getStaff() != null) {
//                destination.setStaffId(source.getStaff().getId());
//                destination.setStaffName(source.getStaff().getUser().getFirstName() + " " +
//                        source.getStaff().getUser().getLastName());
//            }
//            if (source.getAppointment() != null) {
//                destination.setAppointmentId(source.getAppointment().getId());
//            }
//
//            return destination;
//        });
        return modelMapper;
    }
}