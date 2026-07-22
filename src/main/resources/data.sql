-- =============================================
-- B2B_FREIGHT: Commercial B2B & Bulk Freight
-- =============================================
INSERT INTO carriers (name, mode, estimated_days, cost_per_kg, website, category, reliability_score, active_status) VALUES
('TCI Freight', 'Road', 4, 25.0, 'https://www.tcil.com', 'B2B_FREIGHT', 82.0, true),
('VRL Logistics', 'Road', 3, 30.0, 'https://www.vrllogistics.com', 'B2B_FREIGHT', 78.0, true),
('Safexpress', 'Road', 4, 35.0, 'https://www.safexpress.com', 'B2B_FREIGHT', 80.0, true),
('Allcargo Gati', 'Multi', 4, 32.0, 'https://www.allcargologistics.com', 'B2B_FREIGHT', 76.0, true),
('Delhivery Freight', 'Road', 3, 40.0, 'https://www.delhivery.com', 'B2B_FREIGHT', 84.0, true);

-- =============================================
-- E_COMMERCE: E-Commerce & Retail Small Parcels
-- =============================================
INSERT INTO carriers (name, mode, estimated_days, cost_per_kg, website, category, reliability_score, active_status) VALUES
('Delhivery', 'Multi', 3, 75.0, 'https://www.delhivery.com', 'E_COMMERCE', 85.0, true),
('Blue Dart Express', 'Air', 2, 120.0, 'https://www.bluedart.com', 'E_COMMERCE', 90.0, true),
('XpressBees', 'Multi', 3, 52.0, 'https://www.xpressbees.com', 'E_COMMERCE', 80.0, true),
('DTDC Express', 'Road', 4, 45.0, 'https://www.dtdc.in', 'E_COMMERCE', 75.0, true),
('Ecom Express', 'Road', 3, 55.0, 'https://www.ecomexpress.in', 'E_COMMERCE', 78.0, true);

-- =============================================
-- HOUSEHOLD: Packers & Movers
-- =============================================
INSERT INTO carriers (name, mode, estimated_days, cost_per_kg, website, category, reliability_score, active_status) VALUES
('Agarwal Packers', 'Road', 5, 18.0, 'https://www.agarwalpackers.in', 'HOUSEHOLD', 82.0, true),
('Porter Shifting', 'Road', 4, 15.0, 'https://porter.in', 'HOUSEHOLD', 78.0, true),
('NoBroker Move', 'Road', 4, 16.0, 'https://www.nobroker.in/packers-and-movers', 'HOUSEHOLD', 80.0, true),
('Leo Packers', 'Road', 5, 17.0, 'https://www.leopackerandmovers.com', 'HOUSEHOLD', 76.0, true),
('ShiftKarado', 'Road', 4, 14.0, 'https://www.shiftkarado.com', 'HOUSEHOLD', 74.0, true);

-- =============================================
-- COLD_CHAIN: Cold Chain, EXIM & Special Cargo
-- =============================================
INSERT INTO carriers (name, mode, estimated_days, cost_per_kg, website, category, reliability_score, active_status) VALUES
('CONCOR', 'Rail', 6, 12.0, 'https://www.concorindia.co.in', 'COLD_CHAIN', 85.0, true),
('Snowman Logistics', 'Road', 3, 65.0, 'https://www.snowman.in', 'COLD_CHAIN', 88.0, true),
('Allcargo Logistics', 'Multi', 5, 45.0, 'https://www.allcargologistics.com', 'COLD_CHAIN', 82.0, true),
('Kuehne+Nagel India', 'Air', 3, 150.0, 'https://www.kuehne-nagel.com', 'COLD_CHAIN', 92.0, true),
('Gati Kausar', 'Road', 4, 55.0, 'https://www.gkcoldchain.com', 'COLD_CHAIN', 80.0, true);

-- =============================================
-- GENERAL: All-Rounder Carriers (appear in every search)
-- =============================================
INSERT INTO carriers (name, mode, estimated_days, cost_per_kg, website, category, reliability_score, active_status) VALUES
('India Post', 'Rail', 7, 20.0, 'https://www.indiapost.gov.in', 'GENERAL', 65.0, true),
('Professional Couriers', 'Road', 5, 40.0, 'https://www.tpcindia.com', 'GENERAL', 70.0, true),
('FedEx India', 'Air', 2, 200.0, 'https://www.fedex.com/en-in', 'GENERAL', 93.0, true);
