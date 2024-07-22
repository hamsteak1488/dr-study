import React, { useEffect } from 'react';

import formConditions from '@/constants/formConditions';

import { InputWithLabelAndError } from '@/components/molecules/InputWithLabelAndError/InputWithLabelAndError';
import {
  handleKeyDownForNextInput,
  handleKeyDownForSubmit,
} from '../../../_utils/handleKeyDownForNextInput';
import { formWrapperStyles } from '@/components/molecules/Form/Form.styles';

export const StudyDetailStep = ({
  setFocus,
  handleSubmit,
  register,
  errors,
}: any) => {
  // 포커스할 필드명
  useEffect(() => {
    setFocus('max_count');
  }, [setFocus]);

  return (
    <form
      className={formWrapperStyles({ variant: 'steps' })}
      onSubmit={handleSubmit}
    >
      <InputWithLabelAndError
        type="number"
        min={1}
        // max={maxNumber} 추후에 maxNumber 지정 !필요!
        {...register('max_count', {
          ...formConditions.plainText,
          min: '1',
        })}
        errorDisplay={errors?.max_count?.message || ''}
        label="스터디 그룹 최대 인원수"
        onKeyDown={(
          e: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>,
        ) => handleKeyDownForNextInput(e, 'goal_date', setFocus)}
      />
      <InputWithLabelAndError
        type="date"
        {...register('goal_date', { ...formConditions.plainText })}
        errorDisplay={errors?.goal_date?.message || ''}
        label="목표 종료 기간"
        onKeyDown={(
          e: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>,
        ) => handleKeyDownForNextInput(e, 'study_detail', setFocus)}
      />
      <InputWithLabelAndError
        {...register('study_detail', { ...formConditions.plainText })}
        textarea
        errorDisplay={errors?.study_detail?.message || ''}
        label="스터디 그룹 상세내용"
        onKeyDown={(
          e: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>,
        ) => handleKeyDownForSubmit(e, handleSubmit)}
      />
    </form>
  );
};
