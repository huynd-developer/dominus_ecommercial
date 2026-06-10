import dayjs from 'dayjs';
export const formatDate = (date: string | Date | undefined, format = 'DD/MM/YYYYHH:mm') =>
date ? dayjs(date).format(format) : '';

export const formatCurrency = (amount: number | undefined) =>
new Intl.NumberFormat('vi-VN', { style: 'currency', currency:
'VND' }).format(amount || 0);