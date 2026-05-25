import { Modal } from "../modal/Modal";
import RegistrationComponents from "./RegistrationComponents";

export function RegistrationModal({ open, onClose }: { open: boolean, onClose: () => void }) {
  return (
    <Modal open={open} onClose={onClose}>
      <RegistrationComponents />
    </Modal>
  );
}