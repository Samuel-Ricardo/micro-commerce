import { UserDetail } from '../vo/user-detail';

export interface Order {
  id?: string;
  orderNumber?: string;
  skuCode: string;
  price: number;
  quantity: number;
  userDetails: UserDetail;
}
