import { WalletDataInterfaceWithoutSum } from '@app/features/wallet/interfaces';
import Dexie, { type EntityTable } from 'dexie';

export interface User {
    id: number;
    username: string;
}

const db = new Dexie('MoneyTrackerApp') as Dexie & {
    user: EntityTable<User, 'id'>;
    wallet: EntityTable<WalletDataInterfaceWithoutSum, 'id'>;
};

db.version(1).stores({
    user: 'id',
    wallet: 'id',
});

export { db };
