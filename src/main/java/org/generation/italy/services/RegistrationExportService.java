package org.generation.italy.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.generation.italy.model.entities.Activity;
import org.generation.italy.model.entities.Registration;
import org.generation.italy.model.entities.Subject;
import org.generation.italy.model.repositories.RegistrationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationExportService {

    private final RegistrationRepository registrationRepository;

    public RegistrationExportService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(
                    "id",
                    "tipo_registrazione",
                    "dominio",
                    "progetto",
                    "id_soggetto",
                    "tipologia_soggetto",
                    "sessione",
                    "test",
                    "durata_minuti",
                    "data",
                    "medico",
                    "operatori",
                    "data_creazione",
                    "ultima_modifica"
            )
            .build();

    private static final String INTERVENTION_DOMAIN = "Interventi riabilitativi/allenamenti";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<String> toRow(Registration reg) {
        // ordine fisso dei soggetti (per id): cosi id_soggetto e tipologia_soggetto restano paralleli
        List<Subject> orderedSubjects = reg.getSubjects().stream()
                .sorted(Comparator.comparing(Subject::getId))
                .toList();

        return List.of(
                // id
                String.valueOf(reg.getId()),

                // tipo di registrazione: derivato dal dominio (Intervento vs Valutazione)
                INTERVENTION_DOMAIN.equalsIgnoreCase(reg.getDomain().getName()) ? "Intervento" : "Valutazione",

                // dominio (obbligatorio)
                reg.getDomain().getName(),

                // progetto (obbligatorio)
                reg.getProject().getName(),

                // id_soggetto (ordinati per id)
                orderedSubjects.stream()
                        .map(Subject::getCode)
                        .collect(Collectors.joining("; ")),

                // tipologia_soggetto (stesso ordine, duplicati mantenuti per corrispondenza posizionale)
                orderedSubjects.stream()
                        .map(s -> s.getSubjectType().getType())
                        .collect(Collectors.joining("; ")),

                // sessione
                reg.getSession().getSession(),

                // test/attività (lista)
                reg.getActivities().stream()
                        .map(Activity::getName)
                        .collect(Collectors.joining("; ")),

                // durata in minuti
                String.valueOf(reg.getDurationMinutes()),

                // data attività (nullable -> cella vuota se assente)
                reg.getActivityDate() != null
                        ? reg.getActivityDate().format(DATE_FORMAT)
                        : "",

                // medico (nullable -> cella vuota se assente)
                reg.getDoctor() != null
                        ? reg.getDoctor().getFirstName() + " " + reg.getDoctor().getLastName()
                        : "",

                // operatori (lista "Nome Cognome")
                reg.getOperators().stream()
                        .map(op -> op.getFirstName() + " " + op.getLastName())
                        .collect(Collectors.joining("; ")),

                // data/ora creazione (obbligatoria)
                reg.getCreatedAt().format(DATE_TIME_FORMAT),

                // ultima modifica (nullable -> cella vuota se mai modificata)
                reg.getUpdatedAt() != null
                        ? reg.getUpdatedAt().format(DATE_TIME_FORMAT)
                        : ""
        );
    }

    public byte[] exportCsv() {
        // registrazioni ordinate per id crescente
        List<Registration> registrations = registrationRepository.findAllByOrderByIdAsc();

        try (StringWriter writer = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSV_FORMAT)) {

            for (Registration registration : registrations) {
                csvPrinter.printRecord(toRow(registration));
            }

            return writer.toString().getBytes(StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Error while generating CSV", e);
        }
    }

}