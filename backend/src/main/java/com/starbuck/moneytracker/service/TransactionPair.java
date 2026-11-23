package com.starbuck.moneytracker.service;

import com.starbuck.moneytracker.entity.Transaction;

/**
 * Egy párban létrehozott tranzakció két oldala. Csak a service rétegen belül
 * használt (nem API dto), ezért package-private
 *
 * @param srcTransaction a forrás oldali tranzakció
 * @param dstTransaction a cél oldali tranzakció
 */
record TransactionPair(Transaction srcTransaction, Transaction dstTransaction) {}
