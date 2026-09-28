// Fejlécben lévő linkek interface-e. Url és action attributumok kizárják egymást
export interface LinkInterface {
    url?: string;
    langKey: string;
    icon: string;
    action?: () => void
}