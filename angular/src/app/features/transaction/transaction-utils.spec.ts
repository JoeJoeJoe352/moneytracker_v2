import { TransactionUtils } from './transaction-utils';
import { TransactionTypeEnum } from './transaction-type-enum';

describe('TransactionUtils', () => {
    let utils: TransactionUtils;

    beforeEach(() => {
        utils = new TransactionUtils();
    });

    describe('convertDataToInput', () => {
        it('should convert backend INCOME transaction to frontend format', () => {
            const backendData = {
                id: 25,
                name: 'Teszt bevétel',
                priceSum: 5000,
                transactionType: TransactionTypeEnum.INCOME,
                transactionDate: '2024-01-10',
                isComplexTransaction: false,
                transactionDetails: [
                    {
                        name: 'Részlet 1',
                        price: 5000,
                        weight: null,
                        unitPrice: null,
                        isComplexPriceMode: false,
                        categories: [10, 20],
                    },
                ],
            };

            const result = utils.convertDataToInput(backendData);

            expect(result.name).toBe('Teszt bevétel');
            expect(result.isIncome).toBe(true);
            expect(result.price).toBe(5000); // income → pozitív marad
            expect(result.transactionDate instanceof Date).toBe(true);
            expect(result.isComplexTransaction).toBe(false);
            expect(result.categories).toEqual([10, 20]);
            expect(result.details.length).toBe(1);
            expect(result.details[0].name).toBe('Részlet 1');
            expect(result.details[0].price).toBe(5000);
            expect(result.details[0].weight).toBe(null);
            expect(result.details[0].unitPrice).toBe(null);
            expect(result.details[0].isComplexPriceMode).toBe(false);
            expect(result.details[0].categories).toEqual([10, 20]);
        });

        it('should convert backend OUTCOME transaction and normalize prices to positive', () => {
            const backendData = {
                id: 1,
                name: 'Kiadás teszt',
                priceSum: -3000,
                transactionType: TransactionTypeEnum.OUTCOME,
                transactionDate: '2024-01-10',
                isComplexTransaction: true,
                categories: [],
                transactionDetails: [
                    {
                        name: 'Részlet 1',
                        price: -1000,
                        weight: 2,
                        unitPrice: 500,
                        categories: [],
                        isComplexPriceMode: true,
                    },
                ],
            };

            const result = utils.convertDataToInput(backendData);

            expect(result.price).toBe(3000); // outcome → pozitívvá alakítjuk
            expect(result.details[0].price).toBe(1000);
            expect(result.categories).toEqual([]);
        });
    });

    describe('convertDataToInputComplex', () => {
        it('should convert backend INCOME transaction with multiple detail to frontend format', () => {
            const backendData = {
                id: 25,
                name: 'teszt2',
                priceSum: 8000,
                transactionType: TransactionTypeEnum.INCOME,
                transactionDate: '2024-01-10',
                isComplexTransaction: true,
                transactionDetails: [
                    {
                        name: 'Részlet 1',
                        price: 5000,
                        weight: null,
                        unitPrice: null,
                        isComplexPriceMode: false,
                        categories: [10, 20],
                    },
                    {
                        name: 'Részlet 2',
                        price: 3000,
                        weight: 0.5,
                        unitPrice: 6000,
                        isComplexPriceMode: true,
                        categories: [10, 20],
                    },
                ],
            };

            const result = utils.convertDataToInput(backendData);

            expect(result.name).toBe('teszt2');
            expect(result.isIncome).toBe(true);
            expect(result.price).toBe(8000);
            expect(result.transactionDate instanceof Date).toBe(true);
            expect(result.isComplexTransaction).toBe(true);
            expect(result.categories).toEqual([]);
            expect(result.details.length).toBe(2);

            expect(result.details[0].name).toBe('Részlet 1');
            expect(result.details[0].price).toBe(5000);
            expect(result.details[0].weight).toBe(null);
            expect(result.details[0].unitPrice).toBe(null);
            expect(result.details[0].isComplexPriceMode).toBe(false);
            expect(result.details[0].categories).toEqual([10, 20]);

            expect(result.details[1].name).toBe('Részlet 2');
            expect(result.details[1].price).toBe(3000);
            expect(result.details[1].weight).toBe(0.5);
            expect(result.details[1].unitPrice).toBe(6000);
            expect(result.details[1].isComplexPriceMode).toBe(true);
            expect(result.details[1].categories).toEqual([10, 20]);
        });

        it('should convert backend OUTCOME transaction and normalize prices to positive', () => {
            const backendData = {
                id: 1,
                name: 'Kiadás teszt',
                priceSum: -3000,
                transactionType: TransactionTypeEnum.OUTCOME,
                transactionDate: '2024-01-10',
                isComplexTransaction: true,
                categories: [],
                transactionDetails: [
                    {
                        name: 'Részlet 1',
                        price: -1000,
                        weight: 2,
                        unitPrice: 500,
                        categories: [],
                        isComplexPriceMode: true,
                    },
                ],
            };

            const result = utils.convertDataToInput(backendData);

            expect(result.price).toBe(3000); // outcome → pozitívvá alakítjuk
            expect(result.details[0].price).toBe(1000);
            expect(result.categories).toEqual([]);
        });
    });

    describe('convertToBackendData', () => {
        it('should convert simple transaction to backend format', () => {
            const input = {
                name: 'Egyszerű tranzakció',
                price: 2000,
                isIncome: false,
                transactionDate: new Date('2024-01-10'),
                isComplexTransaction: false,
                categories: [{ item_id: 41, item_text: 'Élelmiszer' }],
                details: [
                    {
                        detailName: 'sum',
                        detailPrice: 100,
                        detailWeight: null,
                        detailUnitPrice: null,
                        detailIsComplexPriceMode: false,
                        categories: [],
                    },
                ],
            };

            const result = utils.convertToBackendData(input);

            expect(result.globalPrice).toBe(-2000); // outcome → negatívvá alakítjuk
            expect(result.transactionType).toBe(TransactionTypeEnum.OUTCOME);
            expect(result.transactionDate).toBe('2024-01-10'); // sv-SE formátum
            expect(result.transactionDetails.length).toBe(0);
            expect(result.globalCategories).toEqual([41]);
            expect(result.name).toEqual('Egyszerű tranzakció');
        });

        it('should convert complex transaction details correctly', () => {
            const input = {
                name: 'Komplex tranzakció',
                price: null,
                isIncome: false,
                transactionDate: new Date('2024-01-10'),
                isComplexTransaction: true,
                categories: [],
                details: [
                    {
                        detailName: 'Tétel 1',
                        detailPrice: 1500,
                        detailWeight: null,
                        detailUnitPrice: null,
                        detailIsComplexPriceMode: false,
                        categories: [{ item_id: 5, item_text: 'teszt' }],
                    },
                    {
                        detailName: 'Tétel 2',
                        detailPrice: 0,
                        detailWeight: 3,
                        detailUnitPrice: 100,
                        detailIsComplexPriceMode: true,
                        categories: [{ item_id: 7, item_text: 'teszt2' }, { item_id:8, item_text: 'teszt3' }],
                    },
                ],
            };

            const result = utils.convertToBackendData(input);

            expect(result.globalPrice).toBe(null); // complex → globalPrice nem küldjük
            expect(result.transactionDetails.length).toBe(2);

            // 1. tétel → price mód
            expect(result.transactionDetails[0]).toEqual({
                name: 'Tétel 1',
                price: -1500,
                weight: null,
                unitPrice: null,
                categories: [5],
            });

            // 2. tétel → complex price mode
            expect(result.transactionDetails[1]).toEqual({
                name: 'Tétel 2',
                price: null,
                weight: 3,
                unitPrice: 100,
                categories: [7, 8],
            });
        });
    });
});
