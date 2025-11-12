-- Inserindo contas com saldo zerado
INSERT INTO contas (numero_conta, saldo, version) VALUES ('1001-0', 0.00, 0);
INSERT INTO contas (numero_conta, saldo, version) VALUES ('1002-1', 0.00, 0);

-- Inserindo contas com saldo positivo
INSERT INTO contas (numero_conta, saldo, version) VALUES ('2001-5', 1500.75, 0);
INSERT INTO contas (numero_conta, saldo, version) VALUES ('2002-6', 350.20, 0);
INSERT INTO contas (numero_conta, saldo, version) VALUES ('2003-7', 9800.00, 0);