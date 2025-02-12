import LoginUI from "./login.presenter";
import { useForm } from "react-hook-form";
import { useRouter } from "next/router";
import { errorModal } from "../../lib/util";
import { useState } from "react";
export default function Login() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const router = useRouter();
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isValid },
  } = useForm({
    defaultValues: {
      employeeNumber: "",
      password: "",
    },
    mode: "onChange",
  });
  const onSubmitBoard = async (data) => {
    try {
      console.log(data);
      //rest든 graphql이든 우리 백앤드로 data로 날리는 구간(await)
      router.push(`/wms`);
    } catch (error) {
      if (error instanceof Error) errorModal("fail", error.message);
    }
  };
  const onError = (data) => {
    errorModal("fail", "빈칸없이 입력해주세요");
  };
  const SwitchModal = () => {
    setIsModalOpen((prev) => !prev);
  };
  const onClickJoin = () => {
    router.push(`/login/new`);
  };
  return (
    <LoginUI
      register={register}
      handleSubmit={handleSubmit}
      onSubmitBoard={onSubmitBoard}
      errors={errors}
      onError={onError}
      onClickJoin={onClickJoin}
      isValid={isValid}
      isModalOpen={isModalOpen}
      SwitchModal={SwitchModal}
    />
  );
}
