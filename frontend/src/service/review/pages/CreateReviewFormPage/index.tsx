import React, {
  useState,
  ChangeEvent,
  MouseEvent,
  KeyboardEvent,
  FormEvent,
  useEffect,
} from 'react';
import { flushSync } from 'react-dom';
import { useNavigate, useParams } from 'react-router-dom';

import cn from 'classnames';

import useQuestions from 'service/review/hooks/useQuestions';

import { setFormFocus } from 'service/@shared/utils';

import { Button, Icon, Logo, TextBox } from 'common/components';

import QuestionCard from 'service/review/components/QuestionCard';
import QuestionEditor from 'service/review/components/QuestionEditor';

import styles from './styles.module.scss';

import useReviewFormQueries from './useReviewFormQueries';

function CreateReviewFormPage() {
  const { reviewFormCode } = useParams();
  const navigate = useNavigate();

  const { reviewFormMutation, getReviewFormQuery, initReviewFormData } =
    useReviewFormQueries(reviewFormCode);

  const [reviewTitle, setReviewTitle] = useState(initReviewFormData.reviewTitle);
  const { questions, addQuestion, removeQuestion, updateQuestion } = useQuestions(
    initReviewFormData.questions,
  );

  useEffect(() => {
    if (getReviewFormQuery.isError) {
      alert('존재하지 않는 회고 폼입니다.');
      navigate('/');
    }
  }, []);

  const handleUpdateQuestion = (index: number) => (event: ChangeEvent<HTMLInputElement>) => {
    const updatedQuestion = { questionValue: event.target.value };

    updateQuestion(index, updatedQuestion);
  };

  const handleAddQuestion = ({ currentTarget: $inputTarget }: MouseEvent | KeyboardEvent) => {
    let questionIndex = null;

    flushSync(() => {
      questionIndex = addQuestion({ questionValue: '' });
    });

    if (questionIndex === null) return;

    setFormFocus($inputTarget as HTMLInputElement, questionIndex);
  };

  const handleDeleteQuestion =
    (index: number) =>
    ({ currentTarget: $inputTarget }: MouseEvent | KeyboardEvent) => {
      if (questions.length <= 1) return;

      const previousInputIndex = index - 1;

      removeQuestion(index);
      setFormFocus($inputTarget as HTMLInputElement, previousInputIndex);
    };

  const onChangeReviewTitle = ({ target }: ChangeEvent<HTMLInputElement>) => {
    setReviewTitle(target.value);
  };

  const onClickCreateForm = (event: FormEvent) => {
    event.preventDefault();

    // TODO: 유효성 검증 작성 컨벤션 협의 후 부분 분리
    if (!reviewTitle) {
      alert('회고의 제목을 입력해주세요.');
      return;
    }

    const validQuestions = questions.filter((question) => !!question.questionValue?.trim());
    const removeListKey = validQuestions.map((question) => {
      const newQuestion = { ...question };

      delete newQuestion.listKey;
      return question;
    });

    if (validQuestions.length <= 0) {
      alert('질문은 최소 1개 이상 입력해주세요.');
      return;
    }

    reviewFormMutation.mutate(
      { reviewTitle, reviewFormCode, questions: removeListKey },
      {
        onSuccess: ({ reviewFormCode }) => {
          navigate(`/overview/${reviewFormCode}`, { replace: true });
        },
        onError: ({ message }) => {
          alert(message);
        },
      },
    );
  };

  const onClickCancel = () => {
    if (!confirm('회고 생성을 정말 취소하시겠습니까?\n취소 후 복구를 할 수 없습니다.')) return;

    navigate('/');
  };

  return (
    <>
      <div className={cn(styles.container, 'flex-container column')}>
        <Logo />

        <div className={cn(styles.previewContainer, 'flex-container column')}>
          {questions.map(
            ({ questionValue, questionId, listKey }, index) =>
              questionValue && (
                <QuestionCard
                  key={questionId || listKey}
                  numbering={index + 1}
                  type="text"
                  title={questionValue}
                  description="질문 설명이 이곳에 표기됩니다."
                />
              ),
          )}
        </div>
      </div>

      <div>
        <form className={cn(styles.container, styles.sticky, 'flex-container column')}>
          <TextBox
            theme="underline"
            size="large"
            placeholder="회고의 제목을 입력해주세요."
            value={reviewTitle}
            onChange={onChangeReviewTitle}
          />

          <div className={cn(styles.itemContainer, 'flex-container column')}>
            {questions.map(({ questionId, listKey, questionValue }, index) => (
              <QuestionEditor
                key={questionId || listKey}
                numbering={index + 1}
                value={questionValue}
                onChange={handleUpdateQuestion(index)}
                onAddQuestion={handleAddQuestion}
                onDeleteQuestion={handleDeleteQuestion(index)}
              />
            ))}
          </div>

          <div className={cn('button-container horizontal')}>
            <Button theme="outlined" onClick={onClickCancel}>
              <Icon code="cancel" />
              <span>취소하기</span>
            </Button>

            <Button
              type="button"
              onClick={onClickCreateForm}
              disabled={reviewFormMutation.isLoading}
            >
              <Icon code="drive_file_rename_outline" />
              <span>생성하기</span>
            </Button>
          </div>
        </form>
      </div>
    </>
  );
}

export default CreateReviewFormPage;
