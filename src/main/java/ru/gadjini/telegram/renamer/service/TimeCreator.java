package ru.gadjini.telegram.renamer.service;

import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class TimeCreator {

    public ZonedDateTime now() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }
}
