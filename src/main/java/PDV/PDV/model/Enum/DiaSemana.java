package PDV.PDV.model.Enum;

import java.time.DayOfWeek;

public enum DiaSemana {
    SEGUNDA("Segunda"),
    TERCA("Terça"),
    QUARTA("Quarta"),
    QUINTA("Quinta"),
    SEXTA("Sexta"),
    SABADO("Sábado"),
    DOMINGO("Domingo");

    private final String label;

    DiaSemana(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static DiaSemana from(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> SEGUNDA;
            case TUESDAY -> TERCA;
            case WEDNESDAY -> QUARTA;
            case THURSDAY -> QUINTA;
            case FRIDAY -> SEXTA;
            case SATURDAY -> SABADO;
            case SUNDAY -> DOMINGO;
        };
    }
}
