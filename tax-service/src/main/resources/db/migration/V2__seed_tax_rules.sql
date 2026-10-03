-- Tax Service seed: tax rules, derived from bosch_manager_demo_gst_dataset_20.xlsx (sheet Tax_Rules).
-- hsn_code holds the 4-digit HEADING from the dataset. Exact 8-digit HSNs are NOT invented.
-- Rates are NOT hard-coded anywhere in Java: they live only in this table.
-- The dataset has no cess column; cess_rate is seeded as 0.00 for the verified rules (assumption, see README).
-- Heading 8507 has no verified rate in the dataset, so all its rates are NULL and calculation is refused (HTTP 422).
INSERT INTO tax_rules (tax_rule_id, hsn_code, hsn_level, hsn_description, cgst_rate, sgst_rate, igst_rate, cess_rate, effective_from, effective_to, status, source_reference) VALUES
('TR-8467', '8467', 'HEADING', 'Tools for working in the hand with self-contained motor', 9.00, 9.00, 18.00, 0.00, DATE '2026-10-01', NULL, 'HEADING_RATE_VERIFIED', 'CBIC GST rate schedule'),
('TR-8207', '8207', 'HEADING', 'Interchangeable tools including drilling tools', 9.00, 9.00, 18.00, 0.00, DATE '2026-10-01', NULL, 'HEADING_RATE_VERIFIED', 'CBIC GST rate schedule'),
('TR-8202', '8202', 'HEADING', 'Hand saws; blades for saws of all kinds', 9.00, 9.00, 18.00, 0.00, DATE '2026-10-01', NULL, 'HEADING_RATE_VERIFIED', 'CBIC GST rate schedule'),
('TR-8204', '8204', 'HEADING', 'Hand-operated spanners/wrenches and interchangeable sockets', 9.00, 9.00, 18.00, 0.00, DATE '2026-10-01', NULL, 'HEADING_RATE_VERIFIED', 'CBIC GST rate schedule'),
('TR-8206', '8206', 'HEADING', 'Tools of two or more headings 8202-8205 put up in sets', 9.00, 9.00, 18.00, 0.00, DATE '2026-10-01', NULL, 'HEADING_RATE_VERIFIED', 'CBIC GST rate schedule'),
('TR-8507', '8507', 'HEADING', 'Electric accumulators', NULL, NULL, NULL, NULL, DATE '2026-10-01', NULL, 'REQUIRES_PRODUCT_SPECIFICATION', 'CBIC Circular 163/18/2021-GST');
