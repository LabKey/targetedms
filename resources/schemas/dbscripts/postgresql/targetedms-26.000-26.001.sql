/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Add FKs to Containers and delete orphaned rows
DELETE FROM targetedms.instrumentUsagePayment WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.instrumentSchedule WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.projectPaymentMethod WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.projectResearcher WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.msProject WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.instrumentRate WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.msInstrument WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.paymentMethod WHERE Container NOT IN (SELECT EntityId FROM core.Containers);
DELETE FROM targetedms.rateType WHERE Container NOT IN (SELECT EntityId FROM core.Containers);

ALTER TABLE targetedms.instrumentUsagePayment ADD CONSTRAINT FK_InstrumentUsagePayment_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.msProject ADD CONSTRAINT FK_MSProject_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.projectResearcher ADD CONSTRAINT FK_ProjectResearcher_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.instrumentRate ADD CONSTRAINT FK_InstrumentRate_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.msInstrument ADD CONSTRAINT FK_MSInstrument_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.paymentMethod ADD CONSTRAINT FK_PaymentMethod_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.projectPaymentMethod ADD CONSTRAINT FK_ProjectPaymentMethod_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.instrumentSchedule ADD CONSTRAINT FK_InstrumentSchedule_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);
ALTER TABLE targetedms.rateType ADD CONSTRAINT FK_RateType_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);

-- Add a RateType column to paymentMethod, set its values (creating one if needed), and make it not null
ALTER TABLE targetedms.paymentMethod ADD COLUMN RateType INT;
ALTER TABLE targetedms.paymentMethod ADD CONSTRAINT FK_PaymentMethod_RateType FOREIGN KEY (RateType) REFERENCES targetedms.RateType(Id);

UPDATE targetedms.paymentMethod pm SET RateType = (SELECT MIN(Id) FROM targetedms.RateType rt WHERE rt.Container = pm.Container);

INSERT INTO targetedms.RateType (Name, Container)
    SELECT DISTINCT 'Default', Container FROM targetedms.paymentMethod WHERE RateType IS NULL;

UPDATE targetedms.paymentMethod pm SET RateType = (SELECT MIN(Id) FROM targetedms.RateType rt WHERE rt.Container = pm.Container) WHERE RateType IS NULL;

ALTER TABLE targetedms.paymentMethod ALTER COLUMN RateType SET NOT NULL;