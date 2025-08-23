package org.example.logging.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity(name = "log_dump")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class logDump {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String messageType;

    private String dateTime;

}
