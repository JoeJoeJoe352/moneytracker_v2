/**
 * A legnagyobb érték indexe (egyezésnél az elsőé), üres tömbnél -1.
 * (A Math.max(...values) nagy tömbnél hibát okozhat)
 */
export function indexOfMax(values: number[]): number {
    let maxIndex = -1;
    let maxValue = -Infinity;
    for (const [i, value] of values.entries()) {
        if (maxIndex === -1 || value > maxValue) {
            maxIndex = i;
            maxValue = value;
        }
    }
    return maxIndex;
}
