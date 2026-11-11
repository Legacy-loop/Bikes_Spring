package br.edu.ifc.bikes.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import java.util.TimeZone;

@Configuration
public class SpringTimezoneConfig {

    @PostConstruct //garante que o método seja executado logo após o construtor
                   // da classe inicializada pelo Spring
    public void timezoneConfig(){

        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }
}