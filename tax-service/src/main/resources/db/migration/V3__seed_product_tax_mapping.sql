-- Tax Service seed: product -> HSN heading mapping, derived from sheet Manager_Demo_Data.
-- verification_status is copied verbatim from the dataset. HSN values are headings/candidates, not verified 8-digit codes.
INSERT INTO product_tax_mapping (product_id, hsn_code, hsn_level, hsn_description, verification_status, tax_source, effective_from, effective_to, status) VALUES
('BOS-001', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-002', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-003', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-004', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-005', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-006', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-007', '8467', 'HEADING', 'Hand-held motorized tool classification requires exact product specification', 'VERIFY EXACT HSN', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-008', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'VERIFY EXACT HSN', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-009', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'VERIFY EXACT HSN', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-010', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 'VERIFY EXACT HSN', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-011', '8207', 'HEADING', 'Interchangeable tools including drilling tools', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-012', '8207', 'HEADING', 'Interchangeable tools including drilling tools', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-013', '8207', 'HEADING', 'Interchangeable tools including drilling tools', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-014', '8207', 'HEADING', 'Interchangeable tools including drilling tools', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-015', '8202', 'HEADING', 'Hand saws; blades for saws of all kinds', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-016', '8202', 'HEADING', 'Blades for saws of all kinds', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-017', '8202', 'HEADING', 'Blades for saws of all kinds', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-018', '8204', 'HEADING', 'Hand-operated spanners/wrenches and interchangeable sockets', 'HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-019', '8206', 'HEADING', 'Tools of two or more headings 8202-8205 put up in sets', 'HEADING/RATE VERIFIED; EXACT SET COMPOSITION CHECK', 'CBIC GST rate schedule; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE'),
('BOS-020', '8507', 'HEADING', 'Electric accumulators', 'RATE/EXACT SUBHEADING REQUIRES BATTERY SPECIFICATION', 'CBIC Circular 163/18/2021-GST; GSTN Search HSN', DATE '2026-10-01', NULL, 'ACTIVE');
