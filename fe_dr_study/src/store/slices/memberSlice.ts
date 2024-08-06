import { IMemberData } from '@/interfaces/members';
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

const initialState: IMemberData = {
    id: 0,
    email: '',
    nickname: '',
    imageUrl: '',
};

const memberSlice = createSlice({
    name: 'member',
    initialState,
    reducers: {
        setMemberState: (state, action: PayloadAction<IMemberData>) => {
            state.id = action.payload.id;
            state.email = action.payload.email;
            state.nickname = action.payload.nickname;
            state.imageUrl = action.payload.imageUrl;
        },
        clearMemberState: (state) => {
            state.id = 0;
            state.email = '';
            state.nickname = '';
            state.imageUrl = '';
        },
    },
});

export const { setMemberState, clearMemberState } = memberSlice.actions;
export default memberSlice.reducer;
