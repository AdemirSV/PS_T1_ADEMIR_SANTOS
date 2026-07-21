Feature: Gestion de citas del taller mecanico

  Scenario: Registro exitoso de un mantenimiento ligero con otro mecanico
    Given que existe un mecanico con especialidad MANTENIMIENTO_LIGERO
    When el cliente con placa "SAN-028" agenda una cita de MANTENIMIENTO_LIGERO el dia 18 de setiembre de 2026 a las 14:00
    Then la cita queda registrada con estado PROGRAMADA
    And se notifica que la cita fue agendada exitosamente

  Scenario: Intento de agendamiento con mecanico ocupado iniciando a las 11:00
    Given que el mecanico ya tiene una cita programada el dia 18 de setiembre de 2026 de 10:00 a 12:00
    When el cliente con placa "SAN-028" intenta agendar otra cita con el mismo mecanico a las 11:00
    Then se rechaza el agendamiento afirmando el resultado de superposicion

  Scenario: Intento de agendamiento con mecanico ocupado iniciando a las 12:00
    Given que el mecanico ya tiene una cita programada el dia 18 de setiembre de 2026 de 10:00 a 12:00
    When el cliente con placa "SAN-028" intenta agendar otra cita con el mismo mecanico a las 12:00
    Then se evalua el agendamiento afirmando el resultado real del servicio